# 2026-03-15 Pre-Implementation Documentation Set

## Summary

A top-level documentation set was created under `docs/` to support implementation work from the current repository baseline instead of relying on repeated discovery.

## What Was Added

- `project-overview.md`
- `actors.md`
- `system-modules.md`
- `core-business-flows.md`
- `business-rules.md`
- `data-model.md`
- `state-machine.md`
- `api-contract.md`
- `non-functional-requirements.md`
- `configuration-rules.md`
- `automation-tasks.md`
- `ui-roles.md`

Supporting updates:

- `docs/02_planning/README.md` now stores the documentation-set roadmap instead of a placeholder note.
- `docs/00_index.md` now indexes the expanded doc set.
- `docs/AGENTS.md` now routes future sessions to the right top-level docs by question type.

## Intent

- make current-state versus target-gap documentation explicit
- reduce rediscovery before implementation
- give future sessions a stable place to read actors, flows, rules, data model, API contracts, configuration, and operational expectations

## Verification

- current-state material was derived from the existing source of truth in code and current docs
- future capabilities were labeled as target gaps rather than written as implemented behavior

## How To Reuse This Next Session

- start with `docs/00_index.md`
- use `docs/project-overview.md` for the high-level picture
- use the domain-specific top-level docs before diving back into source code
