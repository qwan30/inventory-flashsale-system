import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { ShellLayout } from "./components/ShellLayout";
import { useAuth } from "./state/auth";
import { BenchmarksPage } from "./views/benchmarks/BenchmarksPage";
import { BenchmarkDetailPage } from "./views/benchmarks/BenchmarkDetailPage";
import { CampaignAuditPage } from "./views/campaigns/CampaignAuditPage";
import { CampaignDetailPage } from "./views/campaigns/CampaignDetailPage";
import { CampaignsPage } from "./views/campaigns/CampaignsPage";
import { LoginPage } from "./views/LoginPage";
import { ChannelHealthPage } from "./views/channels/ChannelHealthPage";
import { OpsPage } from "./views/ops/OpsPage";
import { OpsRemediationPage } from "./views/ops/OpsRemediationPage";

function SessionBootstrappingPage() {
  return (
    <div className="login-screen">
      <div className="login-card">
        <p className="eyebrow">Admin Surface</p>
        <h1>Restoring session</h1>
        <p className="muted">Checking the refresh cookie before loading protected routes.</p>
      </div>
    </div>
  );
}

function ProtectedLayout() {
  const { bootstrapping, session } = useAuth();
  const location = useLocation();

  if (bootstrapping) {
    return <SessionBootstrappingPage />;
  }

  if (!session) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
  }

  return <ShellLayout />;
}

function RoleRedirect({
  allowedRoles,
  redirectTo,
  notice,
}: {
  allowedRoles: Array<"ADMIN" | "OPERATOR">;
  redirectTo: string;
  notice: string;
}) {
  const { session } = useAuth();

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(session.role)) {
    return <Navigate to={redirectTo} replace state={{ notice }} />;
  }

  return <Outlet />;
}

function HomeRedirect() {
  const { session } = useAuth();

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={session.role === "ADMIN" ? "/campaigns" : "/ops"} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<HomeRedirect />} />
        <Route
          element={
            <RoleRedirect
              allowedRoles={["ADMIN"]}
              redirectTo="/ops"
              notice="Campaign management is limited to admin sessions."
            />
          }
        >
          <Route path="/campaigns" element={<CampaignsPage />} />
          <Route path="/campaigns/:campaignId" element={<CampaignDetailPage />} />
          <Route path="/campaigns/:campaignId/audits" element={<CampaignAuditPage />} />
        </Route>
        <Route path="/ops" element={<OpsPage />} />
        <Route path="/ops/remediation" element={<OpsRemediationPage />} />
        <Route path="/channels/health" element={<ChannelHealthPage />} />
        <Route path="/benchmarks" element={<BenchmarksPage />} />
        <Route path="/benchmarks/:runId" element={<BenchmarkDetailPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
