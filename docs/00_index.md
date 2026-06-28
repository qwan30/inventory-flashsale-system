# Documentation Index

## Purpose

This `docs/` tree stores durable project knowledge for the inventory and flash sale system. Use it for information that should survive across sessions, handoffs, and implementation cycles.

Read in this order for broad project work:

1. `docs/AGENTS.md`
2. `docs/00_index.md`
3. `docs/project-overview.md`
4. `docs/system-map.md`
5. `docs/retrieval-guide.md`
6. `docs/actors.md`
7. `docs/system-modules.md`
8. `docs/core-business-flows.md`
9. `docs/business-rules.md`
10. `docs/data-model.md`
11. `docs/state-machine.md`
12. `docs/api-contract.md`
13. `docs/non-functional-requirements.md`
14. `docs/configuration-rules.md`
15. `docs/automation-tasks.md`
16. `docs/ui-roles.md`
17. The newest relevant file in `docs/02_planning/`
18. The newest relevant file in `docs/03_implementation/`
19. The newest relevant file in `docs/05_history/`

## Structure

- Top-level `docs/*.md` files are canonical reference docs and intentionally remain at the root of `docs/`.

- `docs/system-map.md`
  Focused architecture map, main flows, invariants, persistence, and validated behavior.
- `docs/retrieval-guide.md`
  Fast-start guide for where to read first for bugs, features, schema work, or load issues.
- `docs/project-overview.md`
  High-level project statement, current repo summary, target requirement summary, and major gaps.
- `docs/actors.md`
  Business, system, and operational actors that interact with the platform.
- `docs/system-modules.md`
  Ownership and responsibility map for each module in the modular monolith.
- `docs/core-business-flows.md`
  Current and target business flows for reservation, order, expiry, and eventing.
- `docs/business-rules.md`
  Core domain rules and invariants that must hold during implementation changes.
- `docs/data-model.md`
  Current tables, entities, relationships, constraints, and target-only data gaps.
- `docs/state-machine.md`
  Reservation, campaign, order, and outbox states with their allowed transitions.
- `docs/api-contract.md`
  Current public API surface, headers, DTOs, error envelope, and API gaps.
- `docs/non-functional-requirements.md`
  Correctness, consistency, idempotency, observability, and benchmark targets.
- `docs/configuration-rules.md`
  Runtime dependencies, defaults, environment assumptions, and tuning guardrails.
- `docs/automation-tasks.md`
  Current schedulers and future operational automations, clearly labeled by implementation status.
- `docs/ui-roles.md`
  Business and operational roles in this backend-first system.
- `docs/01_ideation/`
  Problem framing, alternatives, constraints, and early design thinking.
- `docs/02_planning/`
  Roadmaps, milestone plans, and execution-ready plans.
- `docs/02_planning/2026-03-15-pre-implementation-doc-set-plan.md`
  Documentation-set plan that established the durable top-level docs taxonomy.
- `docs/02_planning/2026-03-15-broad-execution-roadmap.md`
  Broad post-foundation roadmap sequencing docs catch-up, external alert delivery, benchmark workflow maturity, and the evidence-gated scale audit.
- `docs/02_planning/2026-03-15-shopee-first-sandbox-connector-slice.md`
  Approved Shopee-first sandbox connector plan covering the serial prep bead, two-track execution split, and verification targets for the first real marketplace connector.
- `docs/02_planning/2026-03-15-v1-completion-epic.md`
  Approved V1 completion roadmap covering event contracts, TikTok ingress, admin product surface, benchmark reporting, and guarded scale proof sequencing.
- `docs/02_planning/2026-03-16-admin-ui-screen-spec-and-wireframe-plan.md`
  Lightweight admin UI screen spec and wireframe plan covering the login shell, the core `campaigns`/`ops`/`benchmarks` screens, and the next detail/remediation screens so future frontend work can iterate without a Figma-first pass.
- `docs/02_planning/2026-03-16-admin-ui-missing-screen-specs.md`
  Execution-ready follow-up note listing the still-missing admin UI workflow screens, their route roles, and concise wireframe/spec guidance for detail, audit, remediation, benchmark compare, and channel health views.
- `docs/02_planning/2026-03-16-admin-backend-closure-plan.md`
  Approved backend-first closure plan covering campaign detail, ops remediation reads, and typed benchmark evidence summaries before the next frontend pass.
- `docs/03_implementation/`
  Implementation notes, rollout notes, migration notes, and significant delivery records.
- `docs/03_implementation/2026-03-15-phase-1-hardening-slice.md`
  Focused Phase 1 hardening delivery for outbox retry, metrics, and reservation edge cases.
- `docs/03_implementation/2026-03-15-wave-1-3-monolith-foundation.md`
  Broader monolith-first delivery covering persisted idempotency, channel sync, ops remediation, and reconciliation foundation.
- `docs/03_implementation/2026-03-15-ops-closure-slice.md`
  Operational closure for scheduled reconciliation, alert evaluation, and repeatable benchmark artifact generation.
- `docs/03_implementation/2026-03-15-benchmark-evidence-program.md`
  Benchmark evidence contracts for transient artifacts, curated promotion, and evidence-gated scale decisions.
- `docs/03_implementation/2026-03-15-alert-delivery-and-benchmark-automation-slice.md`
  Delivered Phase A alert delivery plus benchmark runner automation, including persisted alert delivery state, webhook dispatch, auto-promotion, comparison output, and evidence indexing.
- `docs/03_implementation/2026-03-15-admin-security-and-campaign-slice.md`
  Delivered app-managed admin and operator auth, campaign-management APIs, audited admin ops wrappers, and the canonical docs truth-up for the new secure admin surface.
- `docs/03_implementation/2026-03-15-shopee-first-sandbox-connector-slice.md`
  Shipped Shopee connector slice covering real-mode outbound sync, live inbound reconciliation reads, partner-key signing requirements, and full verification evidence.
- `docs/03_implementation/2026-03-15-v1-completion-wave.md`
  Shipped the broad V1 completion wave: versioned outbox contracts, TikTok Shop connector and ingress, benchmark evidence APIs, browser-safe admin auth cookies, provider-aware alert routing, and the React admin UI shell.
- `docs/03_implementation/2026-03-16-admin-backend-closure-slice.md`
  Shipped the backend-first admin closure slice: dedicated campaign detail read, ops remediation history reads, reconciliation run timestamps, and typed benchmark summaries for the next UI workflow pass.
- `docs/03_implementation/2026-03-16-admin-ui-closure-wave.md`
  Shipped the route-first admin UI closure wave covering campaign detail and audit, ops remediation, benchmark detail, refresh-cookie session bootstrap, and role-aware route guards on top of the already-landed admin contracts, while keeping channel health deferred.
- `docs/03_implementation/2026-03-16-production-hardening-channel-health-and-e2e.md`
  Delivered the production-hardening slice that shipped the channel-health backend/UI surface, Playwright browser verification, and the recorded Docker-blocked release-readiness outcome.
- `docs/03_implementation/2026-03-16-ops-copilot-and-simple-cloud-slice.md`
  Delivered the advisory Gemini ops copilot, channel drill-down endpoint, admin UI copilot workflow, and the first simple-cloud packaging plus CI baseline.
- `docs/03_implementation/2026-03-21-gitnexus-project-integration.md`
  Delivered the project-local GitNexus integration for Codex/Claude workflows, including MCP wiring, repo-scoped indexing rules, generated agent context files, and the first verified graph baseline.
- `docs/03_implementation/2026-05-30-docs-truth-refresh.md`
  Source-backed canonical docs refresh for system modules, data model, and core business flows, capturing current TikTok, channel sync, reconciliation, and connector behavior.
- `docs/03_implementation/2026-06-07-cicd-setup.md`
  Delivered comprehensive CI/CD pipeline automation setup for all three projects (Inventory Flashsale, HMS, and Chatbot) including integration testing, API contract verification, Docker image push, and VPS deployment.
- `docs/03_implementation/2026-06-07-readme-professionalization.md`
  Rewrote and professionalized README.md files for all four repositories in the workspace to optimize for job applications, integrating technology badges, Mermaid process diagrams, and verified performance metrics.
- `docs/03_implementation/2026-06-24-readme-refinement.md`
  Refined the main README.md for the inventory system in English to align with the premium, multi-diagram format of the chatbot-hospital-system README.
- `docs/03_implementation/2026-06-24-codebase-cleanup-and-maintenance.md`
  Cleaned unused duplicate frontend views, added comprehensive Javadoc/TSDoc annotations, and overhauled the CI/CD pipeline with path filtering, concurrency gates, Dependabot, and a manual rollback workflow.
- `docs/03_implementation/2026-06-28-codebase-refactoring-and-quality-hardening.md`
  Delivered comprehensive architecture decoupling, FlashSaleCampaign @Version concurrency protection, HexUtils/RestClientUtils refactoring, repo hygiene cleanup, and CI/CD quality gate enhancements.
- `docs/04_audit_remediation/`
  Risk audits, bug investigations, remediation plans, and postmortems.
- `docs/04_audit_remediation/2026-03-15-docs-structure-audit.md`
  File-by-file classification audit for the docs tree and the rationale for the normalized layout.
- `docs/04_audit_remediation/2026-03-15-evidence-gated-scale-audit.md`
  Evidence-backed scale decision record concluding that the system should stay on the modular monolith for now and that replication, partitioning, and lock-safety changes remain gated on stronger benchmark proof.
- `docs/04_audit_remediation/2026-03-16-idea-02-progress-checklist.md`
  Requirement-progress audit mapping the current repo to Idea 02, including a checklist, percentage estimate, and the current Docker-blocked proof gaps.
- `docs/04_audit_remediation/2026-03-21-cv-evidence-and-star-bullets.md`
  Audit note qualifying which benchmark and verification numbers are safe to reuse in a CV, including short STAR-ready Vietnamese bullets and rerun replacement rules.
- `docs/04_audit_remediation/2026-04-27-gitnexus-bmad-source-review.md`
  GitNexus plus BMAD-style full source review documenting implemented capabilities, verification evidence, and current follow-ups for TikTok ingress hardening, Docker-backed backend proof, and stale durable docs.
- `docs/04_audit_remediation/2026-06-07-project-evidence-sheet.md`
  Backend-focused project evidence sheet for resume preparation, classifying verified claims, unsafe claims, measurable scope, ownership evidence, benchmark evidence, and missing proof that future resume-writing work must respect.
- `docs/05_history/`
  Concise dated records of important completed work or important discoveries worth reloading next session.
- `docs/05_history/2026-03-15-docs-structure-normalization.md`
  Durable note describing the docs taxonomy normalization and the planning-folder cleanup.
- `docs/05_history/2026-03-15-session-doc-routing-rules.md`
  Durable note describing how sessions should route planning, implementation, audit, and history output into the correct docs bucket.
- `docs/05_history/2026-03-15-benchmark-evidence-program.md`
  Durable note describing benchmark evidence workflow updates and integration checkpoints.
- `docs/05_history/2026-03-15-first-promoted-benchmark-baseline.md`
  Milestone note identifying the first promoted K6 evidence set and the repo's current informational baseline target for later comparison.
- `docs/05_history/2026-03-15-alert-delivery-and-benchmark-automation.md`
  Milestone note that generic webhook alert delivery and automated benchmark evidence promotion/indexing are now part of the baseline workflow.
- `docs/05_history/2026-03-15-admin-api-foundation.md`
  Milestone note that secure admin and operator APIs now exist and that future admin or ops work must preserve JWT role boundaries and immutable activity audit logging.
- `docs/05_history/2026-03-15-shopee-first-sandbox-connector-slice.md`
  Milestone note that Shopee now has a real sandbox connector path and that future real-mode setups must provide `partnerKey` for signing.
- `docs/05_history/2026-03-15-v1-completion-wave.md`
  Milestone note that event contracts, TikTok omnichannel flows, benchmark reporting APIs, secure browser admin auth, provider-aware alert routing, and the admin UI shell are now part of the repo baseline.
- `docs/05_history/2026-03-16-admin-backend-closure-contracts.md`
  Reload note that future admin UI work should bind to the new campaign detail, ops remediation, and typed benchmark backend contracts directly.
- `docs/05_history/2026-03-16-admin-ui-closure-wave.md`
  Milestone note that the admin UI now has route-first workflow screens beyond the overview shell, reload-safe session bootstrap, and deferred channel health until a dedicated backend summary exists.
- `docs/05_history/2026-03-16-channel-health-and-playwright-baseline.md`
  Reload note that channel health is now a shipped secondary operator workflow and that future release-readiness sessions should check Docker availability first because backend integration and full K6 proof were blocked here.
- `docs/05_history/2026-03-16-idea-02-progress-checkpoint.md`
  Reload note capturing the current overall completion estimate for Idea 02 and the fact that the remaining closure work is now concentrated in omnichannel depth plus Docker-backed proof.
- `docs/05_history/2026-03-16-ops-copilot-and-simple-cloud-baseline.md`
  Reload note that advisory AI ops analysis, channel drill-down, and simple-cloud packaging/CI are now part of the repo baseline, while full Docker-backed backend proof remains open.
- `docs/05_history/2026-03-21-gitnexus-baseline.md`
  Reload note that GitNexus is now part of the repo-local agent workflow baseline and that future sessions can use the indexed code graph after reloading workspace MCP config.
- `docs/05_history/2026-03-15-discovery-and-doc-bootstrap.md`
  History note covering early discovery and document bootstrapping.
- `docs/05_history/2026-03-15-monolith-foundation-wave-1-3.md`
  History record of the monolith foundation delivery wave.
- `docs/05_history/2026-03-15-ops-closure-slice.md`
  History record covering operational closure and scheduled reconciliation.
- `docs/05_history/2026-03-15-pre-implementation-doc-set.md`
  History record establishing pre-implementation documentation taxonomy.
- `docs/05_history/2026-03-21-cv-safe-benchmark-claims.md`
  Reload note capturing the strongest current CV-safe benchmark claims and the guardrails against overstating unproven performance targets.
- `docs/05_history/2026-05-30-gitnexus-codebase-function-scan.md`
  Reload note for the current GitNexus-backed whole-function discovery pass, including the live index baseline, main functional map, best next reads, and verification scope.
- `docs/05_history/2026-06-24-cicd-pipeline-overhaul.md`
  Reload note documenting the CI/CD optimizations, concurrency controls, automated rollback setup, and Dependabot configuration.

## Routing Heuristic

When persisting durable session output:

- planning or orchestration -> `docs/02_planning/`
- implemented delivery -> `docs/03_implementation/`
- audit, investigation, or remediation -> `docs/04_audit_remediation/`
- concise milestone or discovery memory -> `docs/05_history/`
- source-of-truth domain docs -> update the affected top-level `docs/*.md`

## What Belongs Here

- Decisions that affect future implementation.
- System knowledge that would otherwise need to be rediscovered.
- Completed milestones worth remembering.
- Audits and follow-up items that should stay visible.

## What Does Not Belong Here

- Raw chat transcripts.
- Scratch notes with no lasting value.
- Repeated copies of source code.
- AI workspace notes for `skills/` or `everything-claude-code/`.

## History Policy

`docs/05_history/` is for durable signal, not exhaustive logs.

Add a history entry when:

- a meaningful discovery pass changes how future work should start
- a milestone ships
- a bug or risk investigation produces lasting conclusions
- a migration, rollback, or remediation changes operational assumptions

Keep each entry short and include:

- date
- what changed or was learned
- evidence
- what future sessions should do with that knowledge
