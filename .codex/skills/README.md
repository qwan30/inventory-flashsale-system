# Project Skill Workspace

This repository keeps raw third-party and upstream skill sources under `skills/`, then exposes a curated Codex-facing catalog under `.codex/`.

## Layout

- `skills/.agents/skills`
  Source multi-agent skill pack already present in the repo. This is the home of `prompt-leverage`.
- `skills/vendors/ui-ux-pro-max-skill/.claude/skills`
  Vendored UI/UX skill pack from `nextlevelbuilder/ui-ux-pro-max-skill`.
- `skills/vendors/agent-skills/skills`
  Vendored skill pack from `vercel-labs/agent-skills`.
- `everything-claude-code/.agents/skills`
  ECC's Codex-native skill catalog with `openai.yaml` metadata.
- `everything-claude-code/skills`
  ECC's broader library. For this project, the registry curates the Java and Spring Boot subset.
- `.codex/skills/registry.json`
  Machine-readable registry of the available project-local skills and recommended bundles.
- `.codex/scripts/Install-ProjectSkills.ps1`
  Optional installer that copies selected skills from this repo into a Codex skill home such as `$CODEX_HOME/skills`.

## Recommended Bundles

- `core`
  `prompt-leverage`, `ui-ux-pro-max`, `react-best-practices`, `web-design-guidelines`
- `frontend-ux`
  `ui-ux-pro-max`, `design-system`, `ui-styling`, `react-best-practices`, `web-design-guidelines`, `composition-patterns`
- `multi-agent`
  `prompt-leverage`, `planning`, `orchestrator`, `worker`, `knowledge`, `issue-resolution`
- `inventory-backend`
  `prompt-leverage`, `java-coding-standards`, `springboot-patterns`, `jpa-patterns`, `database-migrations`, `springboot-security`, `springboot-tdd`, `springboot-verification`, `security-review`, `verification-loop`
- `ecc-codex`
  `coding-standards`, `tdd-workflow`, `security-review`, `verification-loop`, `deep-research`, `exa-search`, `strategic-compact`, `backend-patterns`, `api-design`

## Notes

- Keep `skills/` isolated from the application build. It is a local AI workspace, not runtime code.
- `everything-claude-code` remains a reference/vendor tree. Integrate through the registry and installer rather than copying its whole `.codex` directory over the project setup.
- Prefer updating vendored sources in place only when the task is explicitly about agent setup.
- If you want Codex to load these as user skills, run the installer script and restart Codex.
