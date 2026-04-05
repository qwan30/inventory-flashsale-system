# 2026-03-21 GitNexus Baseline

- GitNexus is now integrated into this repo as project-local tooling through `.codex/config.toml`, `.gitnexusignore`, `AGENTS.md`, `CLAUDE.md`, and `.claude/skills/gitnexus/`.
- The first graph baseline was created at commit `a6377e2` and verified as up to date with `npx -y gitnexus@latest status`.
- Future analysis sessions can start with GitNexus queries/context instead of only raw file search, but Codex must reload the workspace config before the MCP server appears in-session.
- GitNexus is under `PolyForm Noncommercial`; future sessions should keep that license constraint visible before broader adoption.
