# 2026-03-15 Docs Structure Audit

## Scope

Reviewed every Markdown file under `docs/` and classified it against the repository's existing documentation taxonomy.

## Taxonomy Decisions

- Top-level `docs/*.md` files are canonical reference docs and should remain at the root of `docs/`.
- `docs/01_ideation/` stores early problem framing before a concrete plan exists.
- `docs/02_planning/` stores actionable plans and roadmaps; concrete plans should live in their own files, not only in the folder `README.md`.
- `docs/03_implementation/` stores delivery records and implementation-phase notes.
- `docs/04_audit_remediation/` stores focused audits, findings, and remediation notes.
- `docs/05_history/` stores concise dated milestone and discovery memory.

## Classification Results

Top-level canonical reference docs:

- `docs/00_index.md`
- `docs/AGENTS.md`
- `docs/project-overview.md`
- `docs/system-map.md`
- `docs/retrieval-guide.md`
- `docs/actors.md`
- `docs/system-modules.md`
- `docs/core-business-flows.md`
- `docs/business-rules.md`
- `docs/data-model.md`
- `docs/state-machine.md`
- `docs/api-contract.md`
- `docs/non-functional-requirements.md`
- `docs/configuration-rules.md`
- `docs/automation-tasks.md`
- `docs/ui-roles.md`

Ideation docs:

- `docs/01_ideation/README.md`
- `docs/01_ideation/idea-02-omnichannel-inventory-flash-sale-engine.md`

Planning docs:

- `docs/02_planning/README.md`
- `docs/02_planning/2026-03-15-pre-implementation-doc-set-plan.md`

Implementation docs:

- `docs/03_implementation/README.md`
- `docs/03_implementation/2026-03-15-phase-1-hardening-slice.md`
- `docs/03_implementation/2026-03-15-wave-1-3-monolith-foundation.md`
- `docs/03_implementation/2026-03-15-ops-closure-slice.md`
- `docs/03_implementation/2026-03-15-benchmark-evidence-program.md`

Audit/remediation docs:

- `docs/04_audit_remediation/README.md`
- `docs/04_audit_remediation/2026-03-15-docs-structure-audit.md`

History docs:

- `docs/05_history/README.md`
- `docs/05_history/2026-03-15-discovery-and-doc-bootstrap.md`
- `docs/05_history/2026-03-15-pre-implementation-doc-set.md`
- `docs/05_history/2026-03-15-monolith-foundation-wave-1-3.md`
- `docs/05_history/2026-03-15-ops-closure-slice.md`
- `docs/05_history/2026-03-15-benchmark-evidence-program.md`
- `docs/05_history/2026-03-15-first-promoted-benchmark-baseline.md`

## Findings

- No top-level reference doc was miscategorized. Their placement at the `docs/` root is intentional and matches `docs/00_index.md`.
- The only structural mismatch was `docs/02_planning/README.md`, which contained a concrete plan instead of acting only as a folder guide and index.
- No file currently belongs in `docs/04_audit_remediation/` other than this audit and the folder `README.md`.

## Remediation Applied

- extracted the substantive planning content from `docs/02_planning/README.md` into `docs/02_planning/2026-03-15-pre-implementation-doc-set-plan.md`
- restored `docs/02_planning/README.md` to a folder guide plus current-plan index
- updated stale references that pointed at `docs/02_planning/README.md` as if it were the concrete plan file
- updated the docs index so the root-versus-folder taxonomy is explicit

## Remaining Gap

- There is still no single broad execution roadmap for all post-documentation implementation work. The current planning folder contains the documentation-set plan only.
- Until a broader roadmap is written, use `docs/01_ideation/idea-02-omnichannel-inventory-flash-sale-engine.md` for target direction and the `Still Remaining From The Roadmap` sections in `docs/03_implementation/` for next-slice planning.
