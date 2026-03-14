# Codex Supplement

Use this together with the root `AGENTS.md`.

## Project Shape

- Runtime entrypoint: `apps/api`
- Architecture: Java 21 + Spring Boot 3 modular monolith
- Core modules live under `modules/`
- Ignore `everything-claude-code/` and Maven build output under `target/`

## Docs First

- For broad repo context, read `docs/00_index.md` first.
- For architecture and flow understanding, read `docs/system-map.md`.
- For fast entrypoints into debugging or feature work, read `docs/retrieval-guide.md`.
- When resuming after a gap, check the newest relevant file in `docs/05_history/`.
- Treat `docs/` as the durable project memory; keep `.codex/` focused on Codex bootstrap and workspace-specific guidance.

## Agent Skill Workspace

- For application feature work, treat `skills/` as vendor/reference-only and not part of the build.
- Only enter `skills/` or `everything-claude-code/` when the task is explicitly about Codex setup, prompt engineering, multi-agent workflows, or skill sourcing.
- Primary repo-local skill sources:
  - `skills/.agents/skills` for multi-agent workflows and `prompt-leverage`
  - `skills/vendors/ui-ux-pro-max-skill/.claude/skills` for UI/UX and design-system guidance
  - `skills/vendors/agent-skills/skills` for Vercel frontend and deployment guidance
  - `everything-claude-code/.agents/skills` for Codex-native ECC workflows
  - `everything-claude-code/skills` for the broader ECC library, especially Spring Boot and Java guidance
- Curated registry: `.codex/skills/registry.json`
- Sync script: `.codex/scripts/Install-ProjectSkills.ps1`
- For backend work in this repository, prefer the `inventory-backend` bundle from the registry.

## Default Commands

- Start infrastructure: `docker compose up -d`
- Run tests: `.\mvnw test`
- Run the API app: `.\mvnw -pl apps/api spring-boot:run`

## Engineering Priorities

- Preserve inventory correctness under concurrency and retries.
- Reservation creation, confirmation, release, and expiry must remain idempotent.
- Keep flash-sale quota enforcement and stock reservation logic explicit and testable.
- Prefer module-local domain/application services over pushing business rules into controllers.
- For persistence or messaging changes, keep transaction boundaries and outbox publication behavior coherent.
- When using ECC skills, treat their guidance as secondary to the repository's inventory correctness and concurrency guardrails.

## Testing Expectations

- Add or update tests in the touched module first.
- Use Testcontainers only when behavior crosses MySQL, Redis, or Kafka boundaries.
- Before finishing backend changes, prefer `.\mvnw test`.
