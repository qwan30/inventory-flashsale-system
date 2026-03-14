# Docs Guidance

## Prompt Leverage

- For broad project work, read `docs/00_index.md` first to locate durable project context.
- When a prompt asks for project discovery, architecture explanation, bug triage, or change placement, read `docs/system-map.md` after the index.
- When a prompt asks where to start debugging or implementing a change, read `docs/retrieval-guide.md` after `docs/system-map.md`.
- When a prompt asks about actors, module boundaries, business flows, or responsibilities, consult `docs/actors.md`, `docs/system-modules.md`, and `docs/core-business-flows.md`.
- When a prompt asks about domain invariants or allowed transitions, consult `docs/business-rules.md` and `docs/state-machine.md`.
- When a prompt asks about schema, entities, relationships, or contract shapes, consult `docs/data-model.md` and `docs/api-contract.md`.
- When a prompt asks about operational expectations, performance goals, config, or scheduler behavior, consult `docs/non-functional-requirements.md`, `docs/configuration-rules.md`, and `docs/automation-tasks.md`.
- When a prompt asks about business or operational user responsibilities, consult `docs/ui-roles.md`.
- When resuming after a gap, read the newest relevant entry under `docs/05_history/`.
- Treat these docs as fast-start context, not as a replacement for reading the current source of truth in code.
- If a code change invalidates these docs, update the affected doc in the same task when practical.
- Add a `docs/05_history/` entry only for durable conclusions, important completed milestones, or operationally meaningful discoveries.

## Scope

- Keep documentation in this folder focused on the inventory and flash sale system only.
- Do not add guidance here for `everything-claude-code/` or `skills/`.
