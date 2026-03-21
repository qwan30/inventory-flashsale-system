# 2026-03-16 Ops Copilot And Simple Cloud Slice

## Scope Delivered

This slice advanced the roadmap across the Wave 1 deployment baseline, Wave 2 advisory AI copilot, and Wave 3 operator drill-down surface.

Delivered in code:

- advisory-only ops copilot backend under `/api/v1/admin/ops/copilot/**`
- Gemini provider abstraction with disabled-by-default config and provider metadata in responses
- safe ops-context aggregation for ops overview, benchmark evidence, channel health, and campaign audit scopes
- admin activity audit logging for copilot analysis requests
- operator channel drill-down route:
  - `GET /api/v1/admin/channels/health/{channel}`
- admin UI ops copilot panel wired into the existing ops workflow
- simple-cloud packaging baseline:
  - `apps/api/Dockerfile`
  - `apps/admin-ui/Dockerfile`
  - nginx proxy config for the admin UI container
  - GitHub Actions CI workflow for Java compile plus admin UI test/build/e2e
- README deployment and CI updates for the new container/runtime contract

## Backend Changes

- `ApplicationProperties` now exposes `app.ai.*` and `app.ai.gemini.*` configuration.
- `OpsCopilotService` builds sanitized advisory responses, filters unsupported links and citations, and emits timer/counter metrics.
- `GeminiOpsCopilotProvider` uses the official Gemini REST endpoint through Spring `RestClient` and enforces response budget limits.
- `AdminOpsCopilotController` adds:
  - `GET /api/v1/admin/ops/copilot/capabilities`
  - `POST /api/v1/admin/ops/copilot/analyze`
- `OpsApplicationService` and `AdminChannelController` now expose the read-only channel detail contract.

## Frontend And Deployment Changes

- `apps/admin-ui` now includes an `Ops Copilot` panel in the ops page.
- The UI consumes capabilities plus analysis responses and degrades cleanly when the provider is disabled or returns an error.
- The admin UI container now serves the SPA through nginx and proxies `/api` to a configurable backend host.
- CI now runs:
  - Java package build without tests
  - admin UI unit tests
  - admin UI production build
  - Playwright e2e

## Verification

Passed in this session:

- `.\mvnw.cmd -pl apps/api -am -DskipTests compile`
- `.\mvnw.cmd --% -pl apps/api -am test -Dtest=AdminChannelControllerTest,AdminOpsCopilotControllerTest,GeminiOpsCopilotProviderTest,OpsCopilotServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
- `npm test` in `apps/admin-ui`
- `npm run build` in `apps/admin-ui`
- `npm run test:e2e` in `apps/admin-ui`

Still blocked by environment:

- `.\mvnw.cmd test`
  - API integration tests still depend on Docker/Testcontainers in this workspace
  - the latest full-suite attempt timed out and the failing integration report set still shows Docker environment discovery failures for Testcontainers-backed classes

## Notes For Future Sessions

- Gemini is currently the only implemented provider, but the contract now sits behind `OpsCopilotProvider`.
- The ops copilot is intentionally advisory-only; it does not call retry, replay, resolve, or any mutation API automatically.
- Simple-cloud packaging is now present, but end-to-end deployment proof still needs a real runtime target plus secrets provisioning.
