# 2026-03-15 Session Doc Routing Rules

## Summary

The repository guidance now routes durable session output to a specific `docs/` folder based on task type.

## What Changed

- root `AGENTS.md` now includes a documentation routing matrix and a session-close protocol
- `docs/AGENTS.md` now maps planning, implementation, audit, and history work to their corresponding `docs/` folders
- `.codex/AGENTS.md` now reinforces the same routing rules for Codex bootstrap guidance
- folder `README.md` files now state when each bucket should be used and emphasize dated filenames

## Routing Rules

- planning or orchestration -> `docs/02_planning/`
- implemented delivery -> `docs/03_implementation/`
- audit, review, investigation, or remediation -> `docs/04_audit_remediation/`
- concise milestone or discovery memory -> `docs/05_history/`

## How To Reuse This Next Session

- decide the session's dominant outcome before closing work
- persist one primary durable doc in the matching folder
- add a short `docs/05_history/` note only when the result should be easy to reload across sessions
