# Codex Workspace Notes

- Primary entrypoint: `apps/api`
- Full test command: `.\mvnw test`
- Local infra: `docker compose up -d`
- Repo-local Codex config: `.codex/config.toml`
- Codex-specific guidance: `.codex/AGENTS.md`
- Durable project docs index: `docs/00_index.md`
- Durable system map: `docs/system-map.md`
- Durable retrieval guide: `docs/retrieval-guide.md`
- Durable completion history: `docs/05_history/`
- Project skill registry: `.codex/skills/registry.json`
- Project skill installer: `.codex/scripts/Install-ProjectSkills.ps1`
- ECC integration source: `everything-claude-code/`
- Ignore `everything-claude-code/`
- Treat `skills/` as a local AI workspace only; ignore it for app-code work unless the task is explicitly about Codex/agent setup
