import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Panel } from "../../components/Panel";
import {
  EmptyState,
  InlineError,
  InlineLoading,
  InlineSuccess,
} from "../../components/ResourceState";
import { StatusBadge } from "../../components/StatusBadge";
import { useAsyncAction } from "../../hooks/useAsyncAction";
import { useAsyncResource } from "../../hooks/useAsyncResource";
import {
  activateCampaign,
  endCampaign,
  fetchCampaign,
  type CampaignUpdateRequest,
  updateCampaign,
} from "../../lib/api";
import { formatDateTime, formatNumber } from "../../lib/format";
import { useAuth } from "../../state/auth";

export function CampaignDetailPage() {
  const { campaignId } = useParams();
  const { session } = useAuth();
  const detail = useAsyncResource(
    session && campaignId ? () => fetchCampaign(session.accessToken, campaignId) : null,
    [session?.accessToken, campaignId],
  );
  const action = useAsyncAction();
  const [form, setForm] = useState<CampaignUpdateRequest>({
    startsAt: "",
    endsAt: "",
    quota: 1,
  });

  useEffect(() => {
    if (!detail.data) {
      return;
    }

    setForm({
      startsAt: detail.data.startsAt,
      endsAt: detail.data.endsAt,
      quota: detail.data.quota,
    });
  }, [detail.data]);

  const isDraft = detail.data?.status === "DRAFT";
  const isActive = detail.data?.status === "ACTIVE";
  const isEnded = detail.data?.status === "ENDED";
  const hasChanges = useMemo(() => {
    if (!detail.data) {
      return false;
    }

    return (
      detail.data.startsAt !== form.startsAt ||
      detail.data.endsAt !== form.endsAt ||
      detail.data.quota !== form.quota
    );
  }, [detail.data, form.endsAt, form.quota, form.startsAt]);

  const handleFieldChange = <K extends keyof CampaignUpdateRequest>(
    field: K,
    value: CampaignUpdateRequest[K],
  ) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const runAction = async (request: () => Promise<unknown>, successMessage: string) => {
    await action.run(request, successMessage);
    detail.reload();
  };

  return (
    <div className="stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Campaign workflow</p>
          <h1>Campaign detail</h1>
          <p className="muted">Inspect lifecycle state, quota posture, and safe next actions.</p>
        </div>
        <div className="page-actions">
          <Link className="text-link" to="/campaigns">
            Back to campaigns
          </Link>
          <Link className="text-link" to={`/campaigns/${campaignId}/audits`}>
            Open audit history
          </Link>
        </div>
      </header>
      {detail.loading ? <InlineLoading message="Loading campaign detail..." /> : null}
      {detail.error ? <InlineError message={detail.error} /> : null}
      {!detail.loading && !detail.error && !detail.data ? (
        <EmptyState message="Campaign detail is not available." />
      ) : null}
      {detail.data ? (
        <>
          <Panel
            title={detail.data.id}
            subtitle={`${detail.data.sku} • Created ${formatDateTime(detail.data.createdAt)}`}
          >
            <div className="summary-grid">
              <div className="summary-item">
                <span className="muted">Status</span>
                <div className="summary-inline">
                  <StatusBadge value={detail.data.status} />
                </div>
              </div>
              <div className="summary-item">
                <span className="muted">Starts</span>
                <strong>{formatDateTime(detail.data.startsAt)}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Ends</span>
                <strong>{formatDateTime(detail.data.endsAt)}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Quota</span>
                <strong>{formatNumber(detail.data.quota)}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Reserved quota</span>
                <strong>{formatNumber(detail.data.reservedQuota)}</strong>
              </div>
              <div className="summary-item">
                <span className="muted">Sold quota</span>
                <strong>{formatNumber(detail.data.soldQuota)}</strong>
              </div>
            </div>
          </Panel>

          {action.error ? <InlineError message={action.error} /> : null}
          {action.success ? <InlineSuccess message={action.success} /> : null}

          <div className="grid-2">
            <Panel
              title="Editable campaign fields"
              subtitle={
                isEnded
                  ? "Ended campaigns stay read-only."
                  : "Draft fields can be revised before activation."
              }
            >
              <div className="form-grid">
                <label className="form-field">
                  <span>Starts at</span>
                  <input
                    type="datetime-local"
                    value={toLocalDateTimeInput(form.startsAt)}
                    onChange={(event) =>
                      handleFieldChange("startsAt", toUtcDateTimeString(event.target.value))
                    }
                    disabled={!isDraft || action.pending}
                  />
                </label>
                <label className="form-field">
                  <span>Ends at</span>
                  <input
                    type="datetime-local"
                    value={toLocalDateTimeInput(form.endsAt)}
                    onChange={(event) =>
                      handleFieldChange("endsAt", toUtcDateTimeString(event.target.value))
                    }
                    disabled={!isDraft || action.pending}
                  />
                </label>
                <label className="form-field">
                  <span>Quota</span>
                  <input
                    type="number"
                    min={1}
                    value={form.quota}
                    onChange={(event) =>
                      handleFieldChange("quota", Number.parseInt(event.target.value, 10) || 1)
                    }
                    disabled={!isDraft || action.pending}
                  />
                </label>
              </div>
            </Panel>

            <Panel title="Lifecycle rules" subtitle="Action availability follows current campaign state.">
              <ul className="bullet-list">
                <li>Draft campaigns can be edited and activated.</li>
                <li>Active campaigns can be ended, but not activated again.</li>
                <li>Ended campaigns remain read-only except for audit review.</li>
                <li>Reserved and sold quota are operational facts and never editable.</li>
              </ul>
            </Panel>
          </div>

          <Panel title="Available actions" subtitle="Actions stay inline to keep lifecycle changes deliberate.">
            <div className="action-row">
              {isDraft ? (
                <button
                  className="ghost-button ghost-button-dark"
                  onClick={() =>
                    void runAction(
                      () =>
                        updateCampaign(session!.accessToken, campaignId!, {
                          ...form,
                          quota: Number(form.quota),
                        }),
                      "Draft changes saved.",
                    )
                  }
                  disabled={action.pending || !hasChanges}
                >
                  Save draft changes
                </button>
              ) : null}
              {isDraft ? (
                <button
                  className="primary-button"
                  onClick={() =>
                    void runAction(
                      () => activateCampaign(session!.accessToken, campaignId!),
                      "Campaign activated.",
                    )
                  }
                  disabled={action.pending}
                >
                  Activate campaign
                </button>
              ) : null}
              {isActive ? (
                <button
                  className="danger-button"
                  onClick={() =>
                    void runAction(
                      () => endCampaign(session!.accessToken, campaignId!),
                      "Campaign ended.",
                    )
                  }
                  disabled={action.pending}
                >
                  End campaign
                </button>
              ) : null}
              {isEnded ? (
                <span className="muted">This campaign is ended and can no longer be edited.</span>
              ) : null}
            </div>
          </Panel>
        </>
      ) : null}
    </div>
  );
}

function toLocalDateTimeInput(value: string) {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const timezoneOffset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 16);
}

function toUtcDateTimeString(value: string) {
  if (!value) {
    return "";
  }

  return new Date(value).toISOString();
}
