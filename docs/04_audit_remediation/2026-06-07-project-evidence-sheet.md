# Project Evidence Sheet

Date: 2026-06-07

Evidence source: local repository `D:\projects\inventory-flashsale-system`

Target position: Backend Engineer

Important qualification: Git history in this checkout shows one author, `Thanh Quan <tranthanhquan09@gmail.com>`, across 14 commits. The owner also confirmed solo-developer treatment for this evidence pass. This supports attribution for repository commits, but generated, scaffolded, dependency, and third-party code are not treated as personal engineering contribution.

## 1. Executive project summary

| Item | Finding | Status |
|---|---|---|
| Problem | The project addresses overselling, inconsistent stock views, flash-sale bursts, and delayed order/fulfillment updates in an omnichannel commerce inventory system. | VERIFIED: `docs/project-overview.md`, `README.md` |
| Target users | Backend operators/admins, marketplace operators, and commerce systems that reserve, confirm, release, expire, and reconcile inventory. | VERIFIED for implemented roles and workflows: `docs/actors.md`, `docs/ui-roles.md`, controllers under `apps/api/src/main/java/com/codex/flashsale/controller/` |
| Main solution | Java 21 + Spring Boot 3 modular monolith with MySQL/Flyway persistence, Redis SKU locks, Kafka-backed outbox events, scheduled jobs, marketplace sync/ingress, admin APIs, and a React operator UI. | VERIFIED: `pom.xml`, `apps/api/pom.xml`, `modules/*/pom.xml`, `application.yml`, source modules |
| Architecture | Deployable backend in `apps/api`, bounded modules in `modules/common`, `channel`, `flashsale`, `inventory`, `order`, and `outbox`. | VERIFIED: repo layout, `docs/system-map.md` |
| Current implementation status | Broad V1-style backend exists: reservations, orders, flash-sale campaign rules, outbox, channel sync, reconciliation, alerting, admin auth, benchmark evidence APIs, TikTok/Shopee connector paths, and scheduled jobs. | VERIFIED: source and tests listed below |
| Deployment status | Dockerfiles and GitHub Actions CI exist; local Docker Compose defines MySQL, Redis, Kafka, and Kafka UI. No live production URL or successful deployed runtime proof was found. | VERIFIED + MISSING: `apps/api/Dockerfile`, `apps/admin-ui/Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml` |
| Likely role | Solo developer for this repository checkout, supported by single-author Git history and owner confirmation. | USER-PROVIDED + VERIFIED Git history |
| Strongest evidence | 39 Spring MVC handler methods, 15 migration-created tables, 5 scheduled jobs, 75 Java test methods, 32 admin UI unit/e2e tests, one promoted K6 evidence run with 5 passing scenarios. | VERIFIED: command inspection and referenced files |
| Major gaps | No live URL found, no adoption metrics found, no before/after performance improvement data, full fresh backend integration proof may depend on Docker/Testcontainers availability. | MISSING |

## 2. Repository inspection summary

| Area inspected | Files or artifacts examined | Important findings | Evidence status | Limitations |
|---|---|---|---|---|
| Docs | `README.md`, `docs/00_index.md`, `docs/project-overview.md`, `docs/system-map.md`, `docs/retrieval-guide.md`, prior audit docs | Docs describe a backend-first omnichannel inventory and flash-sale system with known Docker-backed proof gaps. | VERIFIED as documentation, cross-checked with code where practical | Docs are not treated as proof unless implementation exists |
| Source structure | `apps/api`, `modules/*`, `apps/admin-ui` | Modular monolith: deployable Spring Boot app plus six domain/support modules. | VERIFIED | Scope excludes `everything-claude-code/` and `skills/` per repo instructions |
| API surface | `apps/api/src/main/java/com/codex/flashsale/controller/*Controller.java` | 39 Spring MVC handler methods across reservation, inventory, order, admin auth, campaign, ops, benchmark, channel health, and TikTok ingress controllers. | VERIFIED | Count includes 6 legacy `/api/v1/ops` wrappers |
| Data model | `apps/api/src/main/resources/db/migration/*.sql`, JPA entities | 15 migration-created tables, unique idempotency constraints, FK relationships, and indexes for expiry, outbox, channel sync, and audit queries. | VERIFIED | No live database introspection was performed |
| Core backend logic | `ReservationApplicationService`, `InventoryItem`, `FlashSaleCampaign`, `OutboxService`, `ChannelSyncService`, `TikTokIngressService` | Inventory correctness uses Redis SKU lock, transactional state changes, optimistic locking, idempotency records, and outbox/event sync. | VERIFIED | Runtime behavior still depends on integration test execution |
| Security | `SecurityConfiguration`, `AdminAuthService`, admin migrations, auth tests | JWT resource server, BCrypt, refresh token persistence/hash, role-gated admin APIs, audit records, HttpOnly refresh-cookie support. | VERIFIED | TikTok ingress route is public and depends on signature verification; blank secret handling was previously identified as a hardening gap |
| Testing | `apps/api/src/test`, `modules/*/src/test`, `apps/admin-ui/src`, `apps/admin-ui/e2e` | 75 Java test methods, 19 Java API test files, 8 Java module test files, 32 frontend unit/e2e test cases, 7 frontend test/spec files. On 2026-06-07, module tests passed before API integration tests stopped on missing Docker; admin UI unit, build, and Playwright e2e gates passed. | VERIFIED by file/test search and command output | Full backend integration proof remains Docker/Testcontainers-blocked in this shell |
| Performance evidence | `testing/k6/evidence/index.json`, `testing/k6/evidence/20260315-133859-e2e3644/*` | One promoted K6 evidence run passed 5 scenarios at commit `e2e3644`; scenario averages range from 5.57 ms to 164.13 ms. | VERIFIED | No before/after baseline configured; not an improvement metric |
| CI/CD | `.github/workflows/ci.yml`, Dockerfiles | CI packages backend, installs Node dependencies, runs admin UI unit tests, build, and Playwright e2e. Containers exist for API and admin UI. | VERIFIED | CI does not run full Maven tests in workflow |
| Git history | `git shortlog -sne HEAD`, `git log -n 30`, `git remote -v` | 14 commits by `Thanh Quan <tranthanhquan09@gmail.com>`; remote is `https://github.com/thanhquan3010/inventory-flashsale-system.git`. | VERIFIED | Local history may not include all external collaboration outside this checkout |
| GitNexus | `npx.cmd gitnexus status`, `.gitnexus/meta.json`, query output | Index is up to date at commit `b17ee7e`; graph has 3306 nodes, 8978 edges, 262 processes, 0 embeddings. | VERIFIED | Used for discovery, not as sole source of claims |

## 3. Technology verification matrix

| Technology | How it is used | Evidence reference | Depth of usage | Resume relevance | Status |
|---|---|---|---|---|---|
| Java 21 | Backend language and Docker runtime/build target. | `pom.xml`, `apps/api/Dockerfile`, `.github/workflows/ci.yml` | Core | High for backend role | VERIFIED |
| Spring Boot 3 | API application, DI, scheduling, validation, security, JPA integration. | `apps/api/pom.xml`, controllers/services/schedulers | Core | High | VERIFIED |
| Maven multi-module | Builds `apps/api` plus domain modules. | root `pom.xml`, `modules/*/pom.xml` | Core | High | VERIFIED |
| MySQL 8 + Flyway | Durable relational state with 10 migrations and 15 tables. | `docker-compose.yml`, `application.yml`, `db/migration/*.sql` | Core | High | VERIFIED |
| Redis | SKU distributed locking and local infrastructure. | `RedisLockManager`, `ReservationApplicationService`, `application.yml`, `docker-compose.yml` | Core | High | VERIFIED |
| Kafka | Outbox event publishing to `inventory-flashsale.events`. | `OutboxService`, `OutboxPublisherScheduler`, `application.yml`, `docker-compose.yml` | Substantial | High | VERIFIED |
| Spring Security + JWT | Stateless auth, role authorization, JWT encode/decode, BCrypt. | `SecurityConfiguration`, `AdminAuthService`, admin auth tests | Substantial | High | VERIFIED |
| Testcontainers | Backend integration-test infrastructure for MySQL/Redis/Kafka. | `AbstractIntegrationTest.java` | Supporting | Medium | VERIFIED |
| K6 | Benchmark smoke and promoted evidence suite. | `testing/k6/*.js`, `Run-BenchmarkSuite.ps1`, evidence JSON | Substantial | High with qualification | VERIFIED |
| Micrometer | Counters, gauges, and timers for reservation, outbox, and channel sync. | `ReservationApplicationService`, `OutboxService`, `ChannelSyncService` | Supporting | Medium | VERIFIED |
| Docker Compose | Local MySQL, Redis, Kafka, Kafka UI. | `docker-compose.yml` | Supporting | Medium | VERIFIED |
| Docker/Nginx | API and admin UI container packaging. | `apps/api/Dockerfile`, `apps/admin-ui/Dockerfile` | Supporting | Medium | VERIFIED |
| GitHub Actions | CI workflow for backend package and admin UI gates. | `.github/workflows/ci.yml` | Supporting | Medium | VERIFIED |
| React + Vite + TypeScript | Admin/operator UI for campaigns, ops, benchmark, channel health, copilot. | `apps/admin-ui/package.json`, `apps/admin-ui/src/views` | Substantial | Medium for backend role | VERIFIED |
| Vitest + Playwright | Admin UI unit and e2e workflow tests. | `apps/admin-ui/package.json`, `apps/admin-ui/e2e/admin-workflows.spec.ts` | Supporting | Medium | VERIFIED |
| Gemini API | Optional advisory ops copilot provider behind config. | `application.yml`, `apps/api/src/main/java/com/codex/flashsale/ai/` | Supporting | Medium | VERIFIED |
| Shopee/TikTok connector code | Marketplace outbound sync, inbound reconciliation reads, signed TikTok ingress. | connector packages, `TikTokIngressService`, connector integration tests | Substantial | High | VERIFIED |

## 4. Problem, users, and workflows

| Item | Finding | Evidence reference | Status | Confidence |
|---|---|---|---|---|
| Problem | Prevent overselling and inconsistent inventory under flash-sale and marketplace conditions. | `docs/project-overview.md`, `ReservationApplicationService`, `InventoryItem` | VERIFIED | High |
| Intended users | Admins and operators managing campaigns, ops remediation, channel health, and benchmark evidence. | `AdminCampaignController`, `AdminOpsController`, `AdminChannelController`, `AdminBenchmarkController`, `SecurityConfiguration` | VERIFIED | High |
| External systems | Sales channels include `WEB`, `APP`, `SHOPEE`, and `TIKTOK_SHOP`. | `modules/channel/src/main/java/com/codex/flashsale/channel/SalesChannel.java` | VERIFIED | High |
| Reserve workflow | Requires idempotency key, validates channel/campaign, locks SKU, reserves inventory/quota, records outbox, schedules channel sync. | `ReservationController`, `ReservationApplicationService` | VERIFIED | High |
| Confirm workflow | Locks SKU, rejects expired reservation, moves reserved to sold, creates/reuses order, records `order.created`. | `ReservationApplicationService`, `OrderHeader` | VERIFIED | High |
| Release/expire workflow | Returns reserved inventory to available, releases campaign quota, records release event, supports idempotent release. | `ReservationApplicationService`, `ReservationExpiryScheduler` | VERIFIED | High |
| Outbox workflow | Records pending events, scheduled publisher sends Kafka envelope, marks published or failed, supports retry scheduling/manual retry. | `OutboxService`, `OutboxPublisherScheduler`, `AdminOpsController` | VERIFIED | High |
| Channel sync/reconciliation | Persists sync attempts, updates snapshots, compares channel inventory, stores drift/run evidence. | `ChannelSyncService`, `ChannelReconciliationService`, `OpsApplicationService` | VERIFIED | High |
| Admin auth workflow | Login, refresh, logout, seeded admin/operator users, JWT roles, refresh token persistence, audit logs. | `AdminAuthController`, `AdminAuthService`, `V8__admin_security_and_audit.sql` | VERIFIED | High |
| Live user adoption | No active users, traffic, transaction, star, or issue metrics found. | Git/repo inspection | MISSING | High |

## 5. Verified feature and scope inventory

| Feature or capability | Implementation summary | Relevant files | Verified scope | Engineering significance | Status |
|---|---|---|---|---|---|
| Flash-sale reservation lifecycle | Create, confirm, release, expire with inventory/quota movement and state transitions. | `ReservationApplicationService`, `StockReservation`, `InventoryItem`, `FlashSaleCampaign` | 4 core reservation operations | Core inventory correctness | VERIFIED |
| Idempotency | Reservation create uses unique idempotency key; release and order status use operation idempotency records. | `V1__baseline.sql`, `V4__operation_idempotency.sql`, `OperationIdempotencyService` | DB constraints and service replay paths | Prevents duplicate mutation effects | VERIFIED |
| Concurrency control | Redis SKU lock plus JPA optimistic locking on inventory item. | `RedisLockManager`, `InventoryItem`, `ReservationFlowIntegrationTest` | Lock key `lock:inventory:{sku}` and `@Version` | Directly addresses oversell risk | VERIFIED |
| Outbox eventing | Durable event table, versioned envelope, Kafka publish, retry scheduling, admin retry. | `OutboxService`, `OutboxEnvelope`, `V9__outbox_event_version.sql` | Pending/published/failed lifecycle | Reliable integration pattern | VERIFIED |
| Marketplace sync | Sync attempts and snapshots across channels with transient/permanent failures and retry. | `ChannelSyncService`, `ChannelSyncAttempt`, `ChannelInventorySnapshot` | All `SalesChannel` values scheduled from reservation changes | Omnichannel consistency support | VERIFIED |
| Reconciliation | Manual/scheduled runs and drift records without auto-correction. | `ChannelReconciliationService`, `ReconciliationScheduler`, `OpsApplicationService` | Run and drift tables/APIs | Operational safety and auditability | VERIFIED |
| Admin security | Admin/operator roles, JWT access token, refresh token rotation, BCrypt, audit records. | `SecurityConfiguration`, `AdminAuthService`, `V8__admin_security_and_audit.sql` | 2 roles plus auth endpoints | Real backend authorization surface | VERIFIED |
| Admin operations APIs | Alerts, outbox backlog/events/retry, reconciliation runs/drifts/resolve. | `AdminOpsController`, `OpsApplicationService` | 8 admin ops handlers, plus legacy ops wrappers | Operator remediation workflow | VERIFIED |
| Benchmark evidence API | Lists, latest, and detail reads over promoted K6 artifacts. | `AdminBenchmarkController`, `BenchmarkEvidenceService` | 3 handlers | Makes performance proof inspectable | VERIFIED |
| TikTok ingress | Signed inventory and order-status callbacks with receipt dedupe and replay support. | `TikTokIngressController`, `TikTokIngressService`, `V10__tiktok_ingress_receipts.sql` | 2 external ingress handlers plus 1 admin replay handler | Marketplace integration and idempotent ingestion | VERIFIED |
| Scheduled background jobs | Reservation expiry, outbox publish, channel sync, reconciliation, alert delivery. | `apps/api/src/main/java/com/codex/flashsale/scheduler/*.java` | 5 scheduled jobs | Asynchronous backend operations | VERIFIED |
| Admin UI | React UI for campaigns, ops, benchmarks, channel health, ops copilot. | `apps/admin-ui/src/views`, `apps/admin-ui/e2e/admin-workflows.spec.ts` | 7 e2e workflow tests | Supporting evidence for backend API usability | VERIFIED |

## 6. Architecture and design decisions

| Technical problem | Implemented solution | Evidence reference | Trade-off | Engineering significance | Status |
|---|---|---|---|---|---|
| Prevent overselling under concurrent reservations | Serialize SKU mutation through Redis lock and keep `@Version` optimistic lock on `InventoryItem`. | `RedisLockManager`, `ReservationApplicationService`, `InventoryItem` | Requires Redis availability and correct lease tuning | Strong backend concurrency evidence | VERIFIED |
| Avoid duplicate reservation/order mutations | Persist idempotency keys and replay/guard state transitions. | `stock_reservation.idempotency_key`, `operation_idempotency`, service methods | More schema and service complexity | Important distributed API pattern | VERIFIED |
| Keep domain state and integration events consistent | Outbox table stores events in same transactional path before scheduled Kafka publish. | `OutboxService`, migrations, scheduler | Event delivery is asynchronous, not immediate | Production-relevant integration pattern | VERIFIED |
| Support omnichannel stock views without letting channels overwrite central truth | Channel snapshots and reconciliation drifts are stored separately; central inventory remains source of truth. | `ChannelInventorySnapshot`, `ChannelReconciliationService`, docs | Requires operator remediation for drift | Good bounded-context trade-off | VERIFIED |
| Expose remediation without giving all users admin permissions | Role-gated admin/operator APIs and audit records. | `SecurityConfiguration`, `AdminActivityAuditService`, admin tests | Needs role/token lifecycle management | Strong security and ops design | VERIFIED |
| Preserve modular monolith simplicity | Domain modules are separate Maven modules, deployed through one Spring Boot app. | root `pom.xml`, `docs/system-map.md` | Not horizontally split by service | Appropriate until scale proof justifies decomposition | VERIFIED |
| Make performance evidence auditable | K6 evidence artifacts are promoted and served through typed backend APIs. | `testing/k6/evidence`, `BenchmarkEvidenceService` | Evidence is local and historical unless refreshed | Prevents unsupported performance claims | VERIFIED |
| Integrate marketplace callbacks safely | TikTok ingress verifies signed requests and deduplicates receipts before applying effects. | `TikTokIngressController`, `TikTokIngressSignatureVerifier`, `TikTokIngressService` | Public route depends heavily on ingress secret configuration | Meaningful third-party integration evidence | VERIFIED, with hardening caveat |

## 7. Personal contribution analysis

| Contribution | Evidence of ownership | Relevant commits or files | Other contributors involved | Attribution confidence | Status |
|---|---|---|---|---|---|
| Overall repository implementation and documentation | `git shortlog -sne HEAD` shows 14 commits by `Thanh Quan <tranthanhquan09@gmail.com>`; owner confirmed solo-developer treatment. | Git log through `b17ee7e` | None visible in local history | High for this checkout | USER-PROVIDED + VERIFIED Git history |
| Core reservation/concurrency backend | Single-author repo history covers core backend commits; code is in app/module sources. | `ReservationApplicationService`, `InventoryItem`, `FlashSaleCampaign`, `ReservationFlowIntegrationTest` | None visible | High | VERIFIED attribution in local Git |
| Outbox, channel sync, reconciliation, alerts | Single-author commits include monolith idempotency, sync, ops foundation, and alert delivery. | commits `462cd21`, `5496b6a`, `627f0ff`; related services | None visible | High | VERIFIED attribution in local Git |
| Marketplace connectors | Commit `d2a9f7a` says Shopee connector slice; current source also contains TikTok connector/ingress. | connector packages, `TikTokConnectorIntegrationTest`, `ShopeeSandboxConnectorIntegrationTest` | None visible | Medium to High | VERIFIED for source; exact TikTok commit split not exhaustively traced |
| Admin auth and campaign APIs | Commit `60562ac` states secure admin APIs and campaign lifecycle. | `AdminAuthService`, `AdminCampaignApplicationService`, admin tests | None visible | High | VERIFIED attribution in local Git |
| Admin UI | Current history and tests show React UI; backend role relevance is supporting. | `apps/admin-ui/src`, Playwright tests | None visible | Medium | VERIFIED but less central to backend role |
| Third-party libraries/scaffolding | Dependencies and generated framework behavior are not personal contributions. | `pom.xml`, `package.json`, Docker base images | Dependency authors | N/A | Excluded |

## 8. Technical challenges

| Challenge | Why it was difficult | Solution implemented | Evidence | Result | Status |
|---|---|---|---|---|---|
| Oversell prevention | Concurrent flash-sale requests can double-spend limited inventory. | Redis SKU lock, transactional reservation, optimistic locking, integration concurrency test. | `ReservationApplicationService`, `InventoryItem`, `ReservationFlowIntegrationTest` | Implemented and structurally tested; fresh full test result pending environment | VERIFIED |
| Idempotent external APIs | Duplicate HTTP requests and callbacks can replay mutations. | Unique reservation idempotency, operation idempotency, TikTok receipt records. | `V1__baseline.sql`, `V4__operation_idempotency.sql`, `V10__tiktok_ingress_receipts.sql` | Duplicate paths return existing results or conflicts instead of repeating effects | VERIFIED |
| Asynchronous event reliability | State changes and Kafka events must not diverge. | Transactional outbox plus scheduled publisher and retry state. | `OutboxService`, `OutboxPublisherScheduler`, `OutboxEvent` | Events have pending/published/failed lifecycle and retry paths | VERIFIED |
| Marketplace consistency | External channels may lag or disagree with central inventory. | Persisted channel snapshots, sync attempts, reconciliation drifts, operator APIs. | `ChannelSyncService`, `ChannelReconciliationService`, `AdminChannelController` | Central truth is preserved while drift is visible | VERIFIED |
| Role-safe operations | Admin campaign operations and operator remediation need different permissions. | JWT roles and route-level authorization. | `SecurityConfiguration`, `AdminSecurityIntegrationTest` | Admin-only campaigns, admin/operator ops surfaces | VERIFIED |
| Benchmark proof discipline | Resume claims need measured evidence, not target requirements. | Promoted K6 evidence catalog and benchmark read APIs. | `testing/k6/evidence/index.json`, `BenchmarkEvidenceService` | 5 passing local benchmark scenarios exist; no improvement claim is safe | VERIFIED |

## 9. Existing measurable evidence

| Category | Metric | Baseline | Final value | Change | Measurement method | Evidence | Resume-safe? |
|---|---|---|---|---|---|---|---|
| Performance | Promoted K6 suite status | N/A | `PASSED`, 5 scenarios | N/A | `Run-BenchmarkSuite.ps1`, promoted evidence | `testing/k6/evidence/index.json` | Yes, with qualification |
| Performance | Hot SKU contention average latency | No baseline configured | 164.13 ms avg, 920.36 ms p95, 0 failed rate | N/A | K6 report at `baseUrl=http://localhost:8080`, profile `benchmark`, seed stock 100 | `testing/k6/evidence/20260315-133859-e2e3644/report.json` | Yes, with qualification |
| Performance | Flash sale window average latency | No baseline configured | 70.12 ms avg, 220.26 ms p95, 0 failed rate | N/A | Same promoted K6 suite | same report | Yes, with qualification |
| Performance | Reservation expiry average latency | No baseline configured | 48.33 ms avg, 175.51 ms p95, 0 failed rate | N/A | Same promoted K6 suite | same report | Yes, with qualification |
| Performance | Outbox backlog recovery average latency | No baseline configured | 5.57 ms avg, 8.34 ms p95, 0 failed rate | N/A | Same promoted K6 suite | same report | Yes, with qualification |
| Performance | Reconciliation load average latency | No baseline configured | 8.26 ms avg, 25.59 ms p95, 0 failed rate | N/A | Same promoted K6 suite | same report | Yes, with qualification |
| Reliability | Java test methods | N/A | 75 test methods | N/A | `rg '^\\s*@(Test|ParameterizedTest|RepeatedTest)\\b' apps/api/src/test modules` | API/module test files | Yes, with qualification |
| Reliability | Frontend unit/e2e tests | N/A | 32 `it/test` cases across 7 files | N/A | `rg '^\\s*(it|test)\\(' apps/admin-ui/src apps/admin-ui/e2e` | admin UI test/spec files | Yes, with qualification |
| Reliability | Fresh Maven module test execution | N/A | common 1, channel 4, flashsale 2, inventory 2, order 2, outbox 4 passed before API module failure | N/A | `.\mvnw.cmd test` on 2026-06-07 | Maven reactor output | Yes, with qualification |
| Reliability | Fresh full Maven test execution | N/A | API module failed with 9 Testcontainers errors because no valid Docker environment was found | N/A | `.\mvnw.cmd test` on 2026-06-07 | Maven/Testcontainers output | No, blocked |
| Reliability | Fresh backend compile | N/A | Build success for root, six modules, and `apps/api` | N/A | `.\mvnw.cmd -pl apps/api -am -DskipTests compile` on 2026-06-07 | Maven output | Yes |
| Reliability | Fresh admin UI unit tests | N/A | 6 files, 25 tests passed | N/A | `npm test` in `apps/admin-ui` on 2026-06-07 | Vitest output | Yes |
| Reliability | Fresh admin UI e2e tests | N/A | 7 Playwright tests passed | N/A | `npm run test:e2e` in `apps/admin-ui` on 2026-06-07 | Playwright output | Yes, supporting |
| Delivery | Fresh admin UI production build | N/A | TypeScript/Vite build passed; JS bundle 275.76 kB, gzip 83.19 kB | N/A | `npm run build` in `apps/admin-ui` on 2026-06-07 | Vite output | Yes, supporting |
| Scope | Spring controller handlers | N/A | 39 handler methods | N/A | annotation search in controller package | `apps/api/src/main/java/com/codex/flashsale/controller` | Yes, with qualification |
| Scope | Database tables | N/A | 15 `CREATE TABLE` statements | N/A | migration search | `apps/api/src/main/resources/db/migration` | Yes |
| Automation | CI workflow | N/A | Backend package, admin UI unit/build/e2e gates | N/A | YAML inspection | `.github/workflows/ci.yml` | Yes, with qualification |
| Adoption | Users/traffic/revenue | Missing | Missing | Missing | Repo inspection | No source found | No |

## 10. Engineering scope counts

| Scope item | Verified count | Counting method | Exclusions | Evidence | Resume relevance |
|---|---:|---|---|---|---|
| Maven domain/support modules | 6 | Directory count under `modules/` | Excludes `apps/api` deployable and `apps/admin-ui` | `modules/channel`, `common`, `flashsale`, `inventory`, `order`, `outbox` | High |
| Spring MVC handler methods | 39 | Count `@GetMapping`, `@PostMapping`, `@PutMapping`, etc. in controller package | Does not count DTOs or generated routes | controller annotation search | High |
| Migration-created tables | 15 | Count `CREATE TABLE` across Flyway migrations | Excludes indexes-only migrations as tables | `db/migration/*.sql` | High |
| Scheduled jobs | 5 | Count `@Scheduled` annotations | Excludes manual admin triggers | scheduler package | High |
| Java test methods | 75 | Strict JUnit annotation search | Excludes `@Testcontainers` and `@TestPropertySource` | API/module tests | High |
| Java test files | 19 API + 8 module tests | File search under `apps/api/src/test` and `modules/*/src/test` | Excludes generated files | test directories | Medium |
| Frontend tests | 32 cases across 7 files | `it(` and `test(` search | Excludes setup and mocks | `apps/admin-ui/src`, `apps/admin-ui/e2e` | Medium |
| Promoted K6 scenarios | 5 | Evidence report scenario count | Excludes non-promoted artifacts | `testing/k6/evidence/20260315-133859-e2e3644/report.json` | High with qualification |
| User roles | 2 | Role checks in security config and seed user config | Excludes unauthenticated public clients | `SecurityConfiguration`, `application.yml` | High |
| Marketplace/external channels | 2 marketplace channels plus WEB/APP | `SalesChannel` enum | Excludes unimplemented marketplaces | channel module | High |

## 11. Quality, reliability, and security evidence

| Area | Control or practice | Evidence reference | Validation performed | Limitation | Status |
|---|---|---|---|---|---|
| Inventory correctness | Stock arithmetic rejects insufficient reserve, release, confirm conflicts. | `InventoryItem`, `InventoryItemTest` | Source and test inspection | Fresh command result recorded separately | VERIFIED |
| Concurrency | Redis lock around SKU mutation plus optimistic locking. | `ReservationApplicationService`, `RedisLockManager`, `@Version` | Source inspection, GitNexus query, backend compile pass | Full integration test proof requires Docker/Testcontainers | VERIFIED |
| Idempotency | Unique reservation key and operation idempotency records. | migrations, service code | Source/schema inspection | Some behavior depends on integration tests | VERIFIED |
| Transactions | Reservation/confirm/release paths execute through `TransactionTemplate`; services use `@Transactional` where appropriate. | `ReservationApplicationService`, `TikTokIngressService` | Source inspection | No database runtime proof in this document | VERIFIED |
| Event reliability | Outbox persists events and marks publish failures for retry. | `OutboxService`, migrations | Source inspection | Kafka runtime not freshly exercised yet | VERIFIED |
| Auth | JWT, BCrypt, refresh token rotation/hash, role checks. | `SecurityConfiguration`, `AdminAuthService`, admin tests | Source/test inspection; API integration tests could not freshly run without Docker | No independent security audit | VERIFIED |
| Audit logging | Admin activity records for auth and operational actions. | `AdminActivityAuditService`, `V8__admin_security_and_audit.sql` | Source/schema inspection | No external audit trail review | VERIFIED |
| Validation/errors | Domain exceptions and API error handling exist. | `modules/common`, controllers/services | Source inspection | No full API fuzzing | VERIFIED |
| Observability | Micrometer counters/gauges/timers and actuator health/info/metrics. | `ReservationApplicationService`, `OutboxService`, `ChannelSyncService`, `application.yml` | Source/config inspection | No dashboard or alert backend proof found | VERIFIED |
| Security gap | TikTok ingress route is public and relies on signature verification/configured secret. | `SecurityConfiguration`, `TikTokIngressSignatureVerifier`, prior audit | Source inspection | Blank ingress secret handling should be retested/hardened | VERIFIED caveat |

## 12. Delivery and deployment evidence

| Area | Implementation | Evidence | Automation level | Measurable outcome | Status |
|---|---|---|---|---|---|
| Local infra | MySQL 8.4, Redis 7.4, Kafka 3.9.1, Kafka UI through Docker Compose. | `docker-compose.yml` | Manual local startup | No current successful startup proof in this pass yet | VERIFIED config |
| API container | Multi-stage Maven build, Java 21 JRE runtime, port 8080. | `apps/api/Dockerfile` | Container build definition | No image build result in this pass yet | VERIFIED config |
| Admin UI container | Node build stage and nginx runtime. | `apps/admin-ui/Dockerfile` | Container build definition | No image build result in this pass yet | VERIFIED config |
| CI | GitHub Actions builds API package and runs admin UI unit/build/e2e. | `.github/workflows/ci.yml` | Automated on push/PR | No hosted CI run status inspected | VERIFIED config |
| Local verification | Backend compile, admin UI unit/build/e2e passed; full Maven tests blocked at API Testcontainers startup due missing valid Docker environment. | Command output from 2026-06-07 | Manual local verification | Backend integration tests not fully executed | VERIFIED with blocker |
| DB migrations | Flyway enabled with 10 migration files. | `application.yml`, migrations | Automated at app startup | 15 tables defined | VERIFIED |
| Benchmark promotion | K6 runner can promote passing evidence into `testing/k6/evidence`. | `Run-BenchmarkSuite.ps1`, `testing/k6/evidence/index.json` | Scripted local workflow | One promoted passing run exists | VERIFIED |
| Live deployment | No live application URL found. | repo inspection | None found | Not resume-safe | MISSING |

## 13. Adoption and external-impact evidence

| Metric | Value | Observation period | Source | Reliability | Resume-safe? |
|---|---|---|---|---|---|
| Active users | Not found | N/A | Repo inspection | N/A | No |
| Production transactions | Not found | N/A | Repo inspection | N/A | No |
| Revenue/business impact | Not found | N/A | Repo inspection | N/A | No |
| GitHub stars/forks | Not inspected from remote live GitHub | N/A | Local repo only | Unknown | No |
| User feedback/issues | Not found locally | N/A | Repo inspection | N/A | No |
| Deployment analytics | Not found | N/A | Repo inspection | N/A | No |

No adoption or external-impact metric is currently resume-safe.

## 14. Claims that are currently resume-safe

These are evidence claims, not resume bullets.

| Claim | Supporting evidence | Recommended emphasis | Required qualification |
|---|---|---|---|
| Built a Java 21/Spring Boot modular monolith for inventory reservation and flash-sale workflows. | `pom.xml`, `apps/api`, `modules/*`, `docs/system-map.md` | Backend architecture and domain ownership | Attribution supported by local single-author Git history |
| Implemented reservation create/confirm/release/expiry flows with Redis SKU locking and optimistic locking. | `ReservationApplicationService`, `RedisLockManager`, `InventoryItem`, tests | Concurrency and correctness | Do not claim production scale without fresh benchmark proof |
| Designed a relational schema with 15 migration-created tables covering inventory, campaigns, reservations, orders, outbox, idempotency, channel sync, reconciliation, admin auth, audit, alerts, and TikTok receipts. | `apps/api/src/main/resources/db/migration/*.sql` | Data modeling depth | Count is migration table count, not all possible entities |
| Implemented outbox-based Kafka event publication with versioned envelopes, scheduled publishing, failure state, and retry paths. | `OutboxService`, `OutboxEnvelope`, `OutboxPublisherScheduler` | Integration reliability | Avoid "exactly once" unless bounded to tested local behavior |
| Added admin/operator security with JWT access tokens, BCrypt password verification, refresh token rotation, role-gated routes, and audit records. | `SecurityConfiguration`, `AdminAuthService`, `V8__admin_security_and_audit.sql`, admin tests | Security and ops API design | Do not claim independently audited security |
| Integrated marketplace sync/reconciliation for Shopee and TikTok Shop, including signed TikTok ingress and receipt dedupe. | connector packages, `TikTokIngressService`, `V10__tiktok_ingress_receipts.sql`, connector tests | Third-party integration complexity | Qualify real-world usage as unverified |
| Created a benchmark evidence workflow with one promoted K6 run covering 5 scenarios. | `testing/k6/evidence/index.json`, promoted report | Evidence discipline and performance testing | No before/after improvement claim; local benchmark only |
| Maintained automated tests: 75 Java test methods plus 32 frontend unit/e2e cases. | test annotation/case searches | Quality support | Counts are structural unless command verification is attached |
| Added CI and container packaging for API/admin UI. | `.github/workflows/ci.yml`, Dockerfiles | Delivery automation | No production deployment proof found |

## 15. Claims that must not be used yet

| Potential claim | Why it is unsafe or weak | Missing evidence | How it could be validated |
|---|---|---|---|
| "Reduced latency by X%" | No baseline/final comparison exists. | Baseline and post-change measurements under same workload. | Run repeatable K6 baseline and post-change suites, compare p50/p95/avg. |
| "Handles 1000 orders/sec" | `docs/project-overview.md` lists this as a target, not proven current capability. | Throughput benchmark with environment and success criteria. | Run benchmark suite with realistic stock/order data and record throughput/errors. |
| "Production-ready" | Docker-backed backend proof and live deployment proof are missing. | Full integration tests, deployment smoke, monitoring evidence. | Run `.\mvnw test`, container smoke, and deployed health checks. |
| "Used by real customers" | No adoption or user metrics found. | User counts, transaction logs, analytics, or feedback. | Provide verifiable deployment/user evidence. |
| "Fully secure" | No independent security assessment; known TikTok ingress hardening caveat. | Security review, dependency scan, penetration/fuzz testing. | Run security checks and remediate ingress secret behavior. |
| "Fault-tolerant marketplace integration" | Retry/drift handling exists, but production incident evidence is absent. | Failure-injection results and live operational proof. | Run controlled connector failure tests and document recovery. |
| "CI validates all backend tests" | CI packages backend with `-DskipTests`; full Maven tests are not in workflow. | CI job running backend unit/integration tests. | Extend CI or show separate backend test workflow. |
| "Live cloud deployment" | No live URL or deployment run found. | Deployed app URL and smoke test output. | Deploy to target and record health/API/UI evidence. |

## 16. Missing evidence and recommended measurements

| Priority | Missing metric or evidence | Why it matters | Recommended tool | Exact procedure or command | Expected output | Risk level |
|---|---|---|---|---|---|---|
| Critical | Full backend test result | Proves integration workflows with MySQL/Redis/Kafka. | Maven + Docker Desktop/Testcontainers | `docker compose up -d`; `.\mvnw test` | Maven success summary or exact failing tests | Low locally |
| Critical | Fresh benchmark proof | Turns performance from historical local evidence into current evidence. | K6 + benchmark profile | `.\mvnw clean install -DskipTests`; run API with benchmark profile; `.\testing\k6\Run-BenchmarkSuite.ps1 -BaseUrl http://localhost:8080 -SpringProfile benchmark -PromoteIfPassed` | New evidence folder with report/manifest/index entry | Medium, local load only |
| High | Throughput capacity | Needed before claiming `1000 orders/sec` target. | K6/custom benchmark | Add/execute throughput scenario with fixed VUs/ramp, stock seed, and invariant checks. | requests/sec, p95, failed rate, inventory invariant result | Medium |
| High | Backend CI coverage | Current CI skips Maven tests. | GitHub Actions | Add a CI job or command for backend unit tests and Docker-backed integration if feasible. | CI run URL/status | Low to Medium |
| High | TikTok ingress secret hardening | Prevents public route ambiguity. | JUnit/MockMvc | Add tests for blank ingress secret and signed request behavior. | Controlled 401/disabled behavior | Low |
| Medium | Test coverage percentage | Useful for resume only if measured. | JaCoCo/Vitest coverage | Add/run coverage reporting for Java and admin UI. | Coverage report with method/branch/line values | Low |
| Medium | Live deployment proof | Supports deployment claims. | Docker/cloud smoke | Build images, deploy, hit `/actuator/health` and key APIs/UI routes. | URL, status codes, timestamps | Medium |
| Medium | Dependency/security scan | Supports security posture. | Maven/npm audit or SCA | `.\mvnw org.owasp:dependency-check-maven:check` if configured; `npm audit` in admin UI. | Vulnerability report | Low |
| Optional | Adoption metrics | Needed for impact claims. | GitHub/analytics/app logs | Inspect GitHub stars/forks/issues or app analytics for defined period. | Counts with period/source | Low |

## 17. Questions for me

### Critical questions

1. What live URL, if any, should be associated with this project?
   Why it matters: deployment and usage claims are currently `MISSING`.

2. Should `Thanh Quan <tranthanhquan09@gmail.com>` be treated as your resume identity?
   Why it matters: attribution depends on matching Git history to your identity.

3. Was this a solo project throughout, or were there collaborators outside local Git history?
   Why it matters: broad ownership claims should not over-attribute team work.

### Valuable questions

1. What was the development period you want shown on resume materials?
   Why it matters: the Git history here starts on 2026-03-14, but resume timelines may need broader context.

2. Do you have a successful full `.\mvnw test` result from a Docker-enabled machine?
   Why it matters: backend reliability claims become stronger with fresh integration proof.

3. Do you have any current K6 benchmark run after commit `b17ee7e`?
   Why it matters: current performance claims should use current code, not only commit `e2e3644`.

### Optional questions

1. Was this project built for coursework, portfolio, internal demo, or a real business need?
   Why it matters: context affects how resume bullets should frame users and impact.

2. Are Shopee/TikTok connector credentials ever configured against real sandbox accounts?
   Why it matters: integration claims can be qualified as mock/sandbox/real-mode.

3. Do you want the resume writer to emphasize backend only or mention the React admin UI as a supporting full-stack artifact?
   Why it matters: target role is backend, but the UI provides workflow validation evidence.

## 18. Evidence quality score

| Category | Score | Explanation |
|---|---:|---|
| Project context | 4 | Strong docs and matching source structure. |
| Personal ownership | 4 | Owner confirmed solo treatment and Git history shows one author, but external collaboration cannot be disproven. |
| Technical complexity | 4 | Concurrency, idempotency, outbox, channel sync, reconciliation, auth, and connectors are implemented. |
| Functional scope | 4 | Broad API/scheduler/schema/test footprint with meaningful backend workflows. |
| Performance | 3 | Promoted K6 evidence exists, but no before/after comparison or current rerun at latest commit. |
| Testing and reliability | 3 | Many tests and scenarios exist; fresh command proof is required for final confidence. |
| Security | 3 | Real auth/role/token controls exist; no independent audit and one ingress hardening caveat. |
| Deployment and automation | 3 | Docker and CI config exist; no live deployment proof. |
| Adoption or external impact | 0 | No user, traffic, revenue, or external impact evidence found. |
| Overall evidence quality | 3 | Strong technical evidence, limited outcome/adoption evidence, and performance claims require qualification. |

## 19. Handoff package for the Resume Bullet Writer

### Project identity

| Field | Value |
|---|---|
| Project name | Inventory Flash Sale System |
| Project type | Backend-first omnichannel inventory and flash-sale platform |
| Target role | Backend Engineer |
| My role | Solo developer, supported by owner confirmation and single-author local Git history |
| Team size | 1 visible in local Git history |
| Development period | VERIFIED local Git history from 2026-03-14 through 2026-05-31; owner should confirm resume period |
| Live URL | MISSING |
| Repository URL | `https://github.com/thanhquan3010/inventory-flashsale-system.git` |

### Problem and solution

| Field | Value |
|---|---|
| Problem | Overselling, inconsistent omnichannel stock views, flash-sale traffic bursts, and delayed downstream order/inventory updates. |
| Users | Commerce/admin operators, marketplace operators, and external channel systems. |
| Solution | Modular Spring Boot backend with Redis SKU locking, MySQL/Flyway state, Kafka outbox, idempotent reservation/order/channel flows, reconciliation, alerting, benchmark evidence APIs, and an operator UI. |

### Strongest verified technical contributions

| Contribution | Evidence |
|---|---|
| Reservation lifecycle with concurrency safety | `ReservationApplicationService`, `RedisLockManager`, `InventoryItem`, `ReservationFlowIntegrationTest` |
| Transactional outbox and Kafka publish/retry workflow | `OutboxService`, `OutboxPublisherScheduler`, `OutboxEnvelope` |
| Omnichannel sync and reconciliation model | `ChannelSyncService`, `ChannelReconciliationService`, reconciliation migrations |
| Admin/operator security and audit logging | `SecurityConfiguration`, `AdminAuthService`, `V8__admin_security_and_audit.sql` |
| Benchmark evidence workflow and APIs | `testing/k6/evidence`, `BenchmarkEvidenceService`, `AdminBenchmarkController` |

### Strongest verified metrics

| Metric | Evidence |
|---|---|
| 39 Spring MVC handler methods | Controller annotation search |
| 15 migration-created tables | Flyway migration search |
| 75 Java test methods | Strict JUnit annotation search |
| 32 frontend unit/e2e test cases | Admin UI `it/test` search |
| 5 promoted K6 benchmark scenarios passed | `testing/k6/evidence/20260315-133859-e2e3644/report.json` |

### Important technologies with demonstrated usage

| Technology | Evidence of usage | Technical purpose |
|---|---|---|
| Spring Boot 3 / Java 21 | API app, controllers, services, schedulers | Backend platform |
| MySQL + Flyway | 10 migrations, 15 tables | Durable transactional state |
| Redis | SKU lock manager | Concurrency control |
| Kafka | Outbox publisher | Asynchronous integration events |
| Spring Security/JWT/BCrypt | Auth service and security config | Admin/operator access control |
| K6 | Promoted evidence suite | Performance and invariant proof |
| Docker/GitHub Actions | Dockerfiles, Compose, CI YAML | Delivery automation |

### Strongest technical challenges

| Challenge | Solution | Result | Evidence |
|---|---|---|---|
| Prevent overselling | Redis SKU locking plus optimistic locking | Implemented concurrency-safe reservation path | `ReservationApplicationService`, `InventoryItem` |
| Avoid duplicate mutations | Idempotency keys and receipt records | Duplicate reservation/release/ingress paths are guarded | migrations and services |
| Keep events consistent | Transactional outbox with scheduled publish/retry | Durable event lifecycle exists | `OutboxService` |
| Handle external channel drift | Snapshots, reconciliation runs, drifts, operator APIs | Drift is surfaced instead of silently overwriting central inventory | channel reconciliation sources |

### Resume-safe scope

| Verified scope item | Verified count | Evidence |
|---|---:|---|
| Backend controller handlers | 39 | `apps/api/src/main/java/com/codex/flashsale/controller` |
| Database tables | 15 | `apps/api/src/main/resources/db/migration/*.sql` |
| Scheduled jobs | 5 | scheduler annotation search |
| Java test methods | 75 | API/module test search |
| Promoted benchmark scenarios | 5 | K6 evidence report |

### Missing information

| Missing item | Why it matters | Recommended next action |
|---|---|---|
| Live deployment URL | Needed for deployment and adoption claims | Provide URL or deploy and smoke test |
| Current full backend test result | Needed for latest reliability proof | Run `.\mvnw test` with Docker available |
| Current benchmark run at latest commit | Needed for current performance claims | Rerun K6 suite and promote evidence |
| Adoption/user metrics | Needed for business impact claims | Provide verifiable analytics or usage source |

### Warnings for the Resume Bullet Writer

- Do not invent revenue, users, adoption, traffic, availability, or production usage.
- Do not claim latency improvement because there is no baseline/final comparison.
- Do not claim `1000 orders/sec`; it is a target, not verified current performance.
- Do not say "production-ready" without fresh Docker-backed backend tests and deployment smoke evidence.
- Qualify benchmark numbers as local promoted K6 evidence from commit `e2e3644`, not latest production performance.
- Attribute broad work to the project owner only with the local single-author Git history and owner confirmation qualification.
- Treat listed dependencies as resume evidence only where source code shows meaningful usage.
- Mention the TikTok ingress route hardening caveat if making security claims.

## Final quality-control checklist

| Check | Result |
|---|---|
| Every strong claim has an evidence reference. | Done |
| Project-level functionality is not automatically attributed without ownership qualification. | Done |
| No metric has been invented or estimated. | Done |
| Baseline/final values are included for improvement claims. | No improvement claims are made |
| Test or benchmark conditions are documented. | Done for promoted K6 evidence |
| Generated and third-party code are excluded from personal-contribution claims. | Done |
| Technologies are verified through actual implementation. | Done |
| Missing evidence is clearly identified. | Done |
| Measurement recommendations are safe and reproducible. | Done |
| No polished resume bullet has been written. | Done |
| Handoff package is structured for a separate Resume Bullet Writer agent. | Done |
| Fresh `git diff --check` found no whitespace errors. | Done; Git warned `docs/00_index.md` LF will be replaced by CRLF |
| Fresh backend verification was attempted. | `.\mvnw.cmd test` failed only after module tests passed, when API integration tests could not start Testcontainers because Docker was unavailable; `.\mvnw.cmd -pl apps/api -am -DskipTests compile` passed |
| Fresh admin UI verification passed. | `npm test` passed 6 files/25 tests; `npm run build` passed; `npm run test:e2e` passed 7 Playwright tests |
