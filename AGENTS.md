# Codex Repository Instructions

## Project Scope

- Work only inside the root project files for the inventory and flash sale system.
- Ignore `everything-claude-code/` and `skills/`; they are local side repositories and are not part of the build.

## Development Workflow

- Prefer `.\mvnw test` before concluding backend changes.
- Use Docker Compose services from `docker-compose.yml` when local MySQL, Redis, or Kafka are required.
- Keep the app as a modular monolith unless a task explicitly asks to split services.

## Architecture Guardrails

- Inventory correctness is the primary concern.
- Reservation and flash sale flows must remain idempotent and safe under concurrency.
- Cross-module integration should happen through application services or clearly bounded domain services.

