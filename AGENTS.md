# Codex Repository Instructions

- Always use the `prompt-leverage` skill to upgrade the user's initial prompt before executing any tasks.
- Reference `docs/00_index.md` to understand the documentation structure and where durable knowledge should be stored.
- After each completed session, persist durable project knowledge into the appropriate `docs/` bucket using the routing rules below.
- After persisting knowledge, update `docs/00_index.md` to add or update references so that future sessions know what was completed and where to find it.

## Project Scope

- Work only inside the root project files for the inventory and flash sale system.
- Ignore `everything-claude-code/` and `skills/`; they are local side repositories and are not part of the build.

## Development Workflow

- Prefer `.\mvnw test` before concluding backend changes.
- Use Docker Compose services from `docker-compose.yml` when local MySQL, Redis, or Kafka are required.
- Keep the app as a modular monolith unless a task explicitly asks to split services.

## Docs-First Workflow

- Start broad work by reading `docs/00_index.md`, `docs/system-map.md`, and `docs/retrieval-guide.md` before planning or coding.
- If the current client does not support the named slash commands below, follow the equivalent workflow behavior manually.
- During planning and orchestration phases, do not write production code until the user explicitly approves execution.
- When planning output has durable value, persist it into `docs/02_planning/` or `docs/05_history/` as appropriate.

## Task Sizing Rubric

- Classify every non-trivial task before execution as `small bug`, `standard feature`, or `large refactor`.
- Default to the higher-risk category if the size is ambiguous.
- `small bug`: one bounded defect, usually within one module or a small set of files, with no schema change, no major API contract change, and no broad architectural impact.
- `standard feature`: new behavior with clear scope, often touching one or two modules, possibly including API, persistence, or scheduler changes, but still executable as one coherent delivery slice.
- `large refactor`: cross-module or phased work, architectural reshaping, shared contract changes, migration-heavy work, or anything that materially affects concurrency, idempotency, eventing, transaction boundaries, or rollout strategy.

## Default Execution Flows

- `small bug`: docs -> `/tdd` -> `/build-fix` if needed -> `.\mvnw test` -> `/code-review` -> persist durable output using the documentation routing matrix below -> update `docs/00_index.md` if discoverability changed.
- `standard feature`: docs -> `/plan` + orchestrate for multi-agent -> approval gate -> simultaneous agent execution -> `.\mvnw test` -> `/verify` -> `/code-review` -> persist durable output using the documentation routing matrix below -> update `docs/00_index.md` -> `/update-docs`.
- `large refactor`: docs -> `/plan` + orchestrate and decompose into independent tracks -> approval gate -> simultaneous multi-agent execution per track -> `.\mvnw test` -> `/verify quick` -> `/code-review` -> persist durable output using the documentation routing matrix below -> update `docs/00_index.md` -> `/update-docs`.

## Planning And Orchestration Rules

- Planning mode includes orchestration: restate requirements, assess risks, classify task size, decompose work into independent agent tracks, and define verification strategy; no coding in this phase.
- Orchestration happens during planning: decompose work into concurrent tracks by module or responsibility, with each track assigned to a dedicated agent or agent role.
- Assign each track ownership: one agent per track ensures explicit responsibility and minimizes merge risk.
- After planning + orchestration is approved by the user, launch simultaneous multi-agent execution; each agent executes its track independently and reports results.
- Treat agent tracks as autonomous execution units; do not use agents to bypass the approval gate; orchestration must be approved before agents execute.
- For multi-agent runs on `standard feature` tasks, keep orchestration simple: 2 agents max unless task explicitly requires more.
- For large refactors, define track boundaries by architectural concern or module so concurrency and rollout strategy remain clear.

## Knowledge Persistence Rules

After completing any non-trivial task:

- Save durable findings to `docs/02_planning/` (approved plans), `docs/03_implementation/` (delivery records), `docs/04_audit_remediation/` (reviews, audits, investigations), or `docs/05_history/` (discoveries, milestones, concise reload notes).
- Update `docs/00_index.md` to add a reference to the new or updated file under the appropriate section so future sessions can find it.
- Reference should include: date, title, and brief description of what was documented and why it matters to future work.

## Documentation Routing Matrix

Use the task's dominant outcome to choose the primary durable doc location:

- `docs/02_planning/`: approved plans, execution-ready decompositions, phased roadmaps, orchestration notes, and planning outputs that should guide future implementation.
- `docs/03_implementation/`: shipped delivery slices, migration notes, rollout notes, integration assumptions, and verification-backed implementation records after changes land.
- `docs/04_audit_remediation/`: code reviews, bug investigations, risk audits, remediation plans, performance investigations, and postmortems.
- `docs/05_history/`: concise cross-session memory for meaningful discoveries, completed milestones, promoted evidence, or operational conclusions that future sessions should reload quickly.
- top-level `docs/*.md`: canonical reference docs. Update these when the source-of-truth documentation itself changes; do not use them as session logs.

## Session Close Protocol

After every non-trivial completed session:

- Create or update exactly one primary durable doc in `docs/02_planning/`, `docs/03_implementation/`, `docs/04_audit_remediation/`, or `docs/05_history/` based on the routing matrix above.
- Add a second concise note in `docs/05_history/` only when the session shipped a milestone, changed how future work should start, or produced a durable discovery worth reloading quickly.
- If the session included both planning and implementation, persist the reusable plan in `docs/02_planning/` only if it has standalone value, and persist the shipped work in `docs/03_implementation/`.
- If the session was mainly review or investigation, persist the substantive findings in `docs/04_audit_remediation/`; use `docs/05_history/` only for the short cross-session memory of that result.
- Use dated filenames like `YYYY-MM-DD-topic.md` for files under `docs/02_planning/`, `docs/03_implementation/`, `docs/04_audit_remediation/`, and `docs/05_history/`.
- Update `docs/00_index.md` when you add a new durable doc pattern or a new high-signal file that future sessions should discover quickly.

## Architecture Guardrails

- Inventory correctness is the primary concern.
- Reservation and flash sale flows must remain idempotent and safe under concurrency.
- Cross-module integration should happen through application services or clearly bounded domain services.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **inventory-flashsale-system** (3593 symbols, 8299 relationships, 288 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/inventory-flashsale-system/context` | Codebase overview, check index freshness |
| `gitnexus://repo/inventory-flashsale-system/clusters` | All functional areas |
| `gitnexus://repo/inventory-flashsale-system/processes` | All execution flows |
| `gitnexus://repo/inventory-flashsale-system/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
