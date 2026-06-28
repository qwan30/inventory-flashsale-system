# 🚀 Omnichannel Inventory & Flash Sale Concurrency Engine

<div align="center">

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL 8.4](https://img.shields.io/badge/MySQL-8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis 7.4](https://img.shields.io/badge/Redis-7.4-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Apache Kafka 3.9](https://img.shields.io/badge/Apache_Kafka-3.9-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![React 18](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-✓-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![CI Build](https://github.com/thanhquan3010/inventory-flashsale-system/actions/workflows/ci.yml/badge.svg)](https://github.com/thanhquan3010/inventory-flashsale-system/actions)
[![Tests](https://img.shields.io/badge/Tests-100%2B_Passing-22C55E?style=for-the-badge)](https://github.com/thanhquan3010/inventory-flashsale-system/actions)
[![Benchmarks](https://img.shields.io/badge/K6_Benchmarks-Passed-22C55E?style=for-the-badge)](https://github.com/thanhquan3010/inventory-flashsale-system)
[![Release](https://img.shields.io/badge/Release-v1.0-0d7c4b?style=for-the-badge)](https://github.com/thanhquan3010/inventory-flashsale-system)

**An enterprise-grade omnichannel inventory reservation and flash sale concurrency engine** built as a high-performance Modular Monolith using Java 21 and Spring Boot 3. It utilizes Redis distributed locking, JPA optimistic locking, and the Transactional Outbox pattern with Apache Kafka to guarantee strict stock correctness, eliminate overselling, and maintain multi-channel synchronization under heavy traffic contention. Featuring a clean React admin dashboard for campaign management, operational drift reconciliation, and real-time load test analytics.

> **🟢 Production Status: v1.0 — June 2026**
> 100+ backend/frontend assertions passing. 8/8 K6 load benchmark suites passed. 2 CI/CD automation workflows active via GitHub Actions with image builds pushed to GitHub Container Registry (GHCR) and automated VPS deployment.
> 
> 📂 **[Durable Documentation Index →](docs/00_index.md)** | 📋 **[System Map & API Contract →](docs/system-map.md)** | 🔒 **[Business Rules & Invariants →](docs/business-rules.md)**

</div>

---

## 🎯 Key Features & Business Value

| # | Business Domain | Technical Implementation | Business Impact |
|---|-----------------|-------------------------|-----------------|
| ⚡ | **Overselling Prevention** | Redis distributed locking per SKU (`lock:inventory:{sku}`) + JPA optimistic locking (`@Version`) | Eliminates overselling completely under intense Hot SKU contention during flash sales |
| 📬 | **Transactional Eventing** | Transactional Outbox Pattern: saves state and events atomically in one MySQL transaction; published to Apache Kafka | Prevents event loss or inconsistency between database state and downstream message brokers |
| 🔄 | **Omnichannel Inventory Sync** | Async snapshotting, sync attempts queue, and scheduled Reconciliation sweeps | Ensures inventory levels on TikTok Shop and Shopee reflect physical central stock, resolving lags |
| 🔐 | **Strict Idempotency** | Persistent Idempotency Ledger (`operation_idempotency` checks both key and value) | Avoids duplicate orders or reservation releases when buyers re-submit HTTP calls or webhooks retry |
| 📈 | **Automated Benchmarks** | Integrated K6 test suite modeling 8 distinct load profiles (contention, expiry, outbox recovery) | Proves high-throughput latency baselines and lock safety before production deployment |
| 📊 | **Operational Controls** | Web-based admin dashboard in React, Vite, and Tailwind CSS | Empowers operators with campaign management, outbox backlog retries, and drift resolution |
| 🛡 | **Secured Management** | JWT-based role-based access control (RBAC) with browser-safe HttpOnly cookies and immutable activity audit | Restricts sensitive actions to authenticated operators while generating clear audit trails |

---

## 🏗️ System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        U["👤 Operational Admins / Webhooks / K6 Clients"]
        N["🔀 Nginx Ingress Proxy"]
    end

    subgraph "Application Core (apps/api)"
        FE["⚛️ React Admin Portal<br/><i>Vite · TypeScript · Tailwind</i>"]
        BE["☕ Spring Boot Core App<br/><i>Java 21 · Modular Monolith</i>"]
    end

    subgraph "Bounded Domain Modules (modules/*)"
        common["📦 common<br/><i>Exceptions · Core Types</i>"]
        channel["🔌 channel<br/><i>Shopee & TikTok Connectors</i>"]
        flashsale["⚡ flashsale<br/><i>Campaign Quota & Windows</i>"]
        inventory["🗄️ inventory<br/><i>Stock Allocation & Locks</i>"]
        order["🛒 order<br/><i>Lifecycle & Status Transitions</i>"]
        outbox["📬 outbox<br/><i>Transactional Event Logger</i>"]
    end

    subgraph "Data & Infrastructure Layer"
        DB[("🐬 MySQL 8.4<br/><i>Flyway · 15 Tables · ACID</i>")]
        RD[("🗄️ Redis 7.4<br/><i>SKU Distributed Locks</i>")]
    end

    subgraph "Eventing & Messaging Layer"
        KF[("🎛️ Apache Kafka 3.9<br/><i>Event Broker</i>")]
        KUI["🖥️ Kafka-UI<br/><i>Topic Inspector</i>"]
    end

    U --> N
    N -->|"/api/v1/admin/*"| BE
    N -->|"/"| FE
    FE -->|"/api/v1/*"| BE
    BE --> common
    BE --> channel
    BE --> flashsale
    BE --> inventory
    BE --> order
    BE --> outbox
    inventory --> DB
    inventory --> RD
    outbox --> DB
    outbox --> KF
    KF --> KUI

    style U fill:#1e40af,stroke:#3b82f6,color:#fff
    style N fill:#ea580c,stroke:#fb923c,color:#fff
    style FE fill:#000,stroke:#666,color:#fff
    style BE fill:#059669,stroke:#34d399,color:#fff
    style DB fill:#1e40af,stroke:#60a5fa,color:#fff
    style RD fill:#dc2626,stroke:#f87171,color:#fff
    style KF fill:#111,stroke:#666,color:#fff
    style KUI fill:#4b5563,stroke:#9ca3af,color:#fff
```

---

## 📐 Concurrency-Safe Reservation Flow

This sequence diagram illustrates how the system coordinates the idempotency check, Redis distributed locking, and database ACID transactions to ensure stock correctness:

```mermaid
sequenceDiagram
    autonumber
    actor C as 👤 Client / Buyer
    participant API as 🚪 Ingress API Gateway (Spring Boot)
    participant Redis as 🗄️ Redis Lock Manager
    participant DB as 🐬 MySQL DB (ACID Transaction)
    participant OB as 📬 Outbox Publisher (Spring)
    participant KF as 🎛️ Kafka Broker

    C->>API: POST /api/v1/flash-sales/{campaignId}/reservations (Idempotency Key)
    API->>API: Verify Idempotency ledger (operation_idempotency)
    API->>Redis: Acquire SKU lock (lock:inventory:{sku})
    
    alt Lock Acquired
        Redis-->>API: Success (Lock lease: 5s)
        API->>DB: Start transaction
        API->>DB: Check inventory & campaign window/quota
        API->>DB: Update available/reserved quantities (Optimistic Version Check)
        API->>DB: Persist StockReservation (ACTIVE)
        API->>DB: Write event inventory.reservation.created to outbox_event
        DB-->>API: Commit Transaction (ACID)
        API->>Redis: Release SKU lock
        API-->>C: Return 201 Created (Reservation Details)
    else Lock Timeout / Contention
        Redis-->>API: Lock Failed (Timeout after 2s)
        API-->>C: Return 409 Conflict / 429 Too Many Requests
    end

    loop Outbox Scheduler (Every 5s)
        OB->>DB: Fetch PENDING outbox events (batch size: 50)
        OB->>KF: Publish event envelope to inventory-flashsale.events
        KF-->>OB: Acknowledge (Record metadata)
        OB->>DB: Update outbox status to PUBLISHED
    end
```

---

## 🔄 CI/CD Pipeline

The repository integrates a comprehensive 2-stage workflow using GitHub Actions to compile the monolith, test the frontend, package container images, and deploy:

```mermaid
graph TD
    subgraph "CI Pipeline (GitHub Actions)"
        Trigger["Push / PR to main"]
        Checkout["actions/checkout@v5"]
        SetupJava["actions/setup-java@v4"]
        MvnVerify[mvn clean verify]
        SetupNode["actions/setup-node@v4"]
        NpmCi["npm ci apps/admin-ui"]
        Vitest["npm test admin-ui"]
        BuildUI["npm run build admin-ui"]
        Playwright["npm run test:e2e admin-ui"]
        DockerLogin["docker/login-action@v3"]
        BuildBackend["Build & Push Backend Image"]
        BuildFrontend["Build & Push Frontend Image"]
    end

    subgraph "CD Pipeline (Auto-deploy on Success)"
        CDTrigger["CI completed successfully"]
        SCPCompose["SCP docker-compose.yml to server"]
        SSHDeploy["SSH to deployment VPS"]
        PullImages["docker compose pull"]
        UpContainers["docker compose up -d"]
        PruneDangling["docker image prune -f"]
    end

    Trigger --> Checkout
    Checkout --> SetupJava
    SetupJava --> MvnVerify
    MvnVerify --> SetupNode
    SetupNode --> NpmCi
    NpmCi --> Vitest
    Vitest --> BuildUI
    BuildUI --> Playwright
    Playwright --> DockerLogin
    DockerLogin --> BuildBackend
    DockerLogin --> BuildFrontend
    BuildBackend --> CDTrigger
    BuildFrontend --> CDTrigger
    CDTrigger --> SCPCompose
    SCPCompose --> SSHDeploy
    SSHDeploy --> PullImages
    PullImages --> UpContainers
    UpContainers --> PruneDangling
```

---

## 🗄️ Database Entity Relationship

The database schema is partitioned logically by boundaries corresponding to our domain modules, enforced via foreign keys:

```mermaid
erDiagram
    inventory_item ||--o{ flash_sale_campaign : configures
    inventory_item ||--o{ stock_reservation : reserves
    flash_sale_campaign ||--o{ stock_reservation : scopes
    stock_reservation ||--|| order_header : creates
    outbox_event ||--o{ channel_sync_attempt : triggers
    outbox_event ||--o{ channel_inventory_snapshot : snapshot_source
    inventory_reconciliation_run ||--o{ inventory_reconciliation_drift : records
    admin_user ||--o{ admin_refresh_token : auths

    inventory_item {
        varchar sku PK
        int available_qty
        int reserved_qty
        int sold_qty
        bigint version
    }
    flash_sale_campaign {
        varchar id PK
        varchar sku FK
        timestamp starts_at
        timestamp ends_at
        int quota
        int reserved_quota
        int sold_quota
        varchar status
    }
    stock_reservation {
        varchar id PK
        varchar sku FK
        varchar campaign_id FK
        varchar channel
        int quantity
        varchar status
        timestamp expires_at
        varchar idempotency_key
        varchar confirm_idempotency_key
        varchar order_id
    }
    order_header {
        varchar id PK
        varchar reservation_id FK
        varchar channel
        varchar status
    }
    outbox_event {
        varchar id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        text payload
        varchar status
        int attempts
        timestamp published_at
        varchar last_error
        int event_version
    }
    channel_sync_attempt {
        varchar id PK
        varchar outbox_event_id FK
        varchar channel
        varchar sku
        varchar event_type
        text payload
        varchar status
        int attempts
        timestamp synced_at
    }
    channel_inventory_snapshot {
        varchar id PK
        varchar channel
        varchar sku
        int available_qty
        int reserved_qty
        int sold_qty
        varchar source_outbox_event_id FK
        timestamp synced_at
    }
    inventory_reconciliation_run {
        varchar id PK
        varchar trigger_type
        int scanned_sku_count
        int scanned_snapshot_count
        int open_drift_count
        varchar status
        varchar failure_message
        timestamp completed_at
    }
    inventory_reconciliation_drift {
        varchar id PK
        varchar run_id FK
        varchar channel
        varchar sku
        int central_available_qty
        int central_reserved_qty
        int central_sold_qty
        int observed_available_qty
        int observed_reserved_qty
        int observed_sold_qty
        varchar status
        varchar resolution_note
        timestamp resolved_at
    }
    admin_user {
        varchar id PK
        varchar username
        varchar password_hash
        varchar display_name
        varchar role
        boolean enabled
    }
    admin_refresh_token {
        varchar id PK
        varchar user_id FK
        varchar token_hash
        timestamp expires_at
        timestamp revoked_at
    }
    admin_activity_audit {
        varchar id PK
        varchar actor_username
        varchar actor_role
        varchar action
        varchar resource_type
        varchar resource_id
        varchar outcome
        varchar correlation_id
        text details
    }
    alert_delivery_state {
        varchar alert_code PK
        varchar last_observed_status
        timestamp last_observed_at
        varchar last_notified_status
        timestamp last_sent_at
        varchar last_error
        int consecutive_failures
    }
    channel_ingress_receipt {
        varchar id PK
        varchar channel
        varchar receipt_type
        varchar external_receipt_id
        varchar payload_hash
        varchar outcome
        timestamp processed_at
    }
    operation_idempotency {
        varchar id PK
        varchar operation_type
        varchar resource_id
        varchar operation_value
        varchar idempotency_key
        text response_payload
    }
```

---

## 🚢 Deployment Architecture

```mermaid
graph TB
    subgraph "Client Access"
        C["👤 Operators / Buyers"]
    end

    subgraph "VPS Server Environment (Docker Compose)"
        NG["🔀 Nginx Ingress Proxy<br/><i>Port :80 / :443</i>"]
        FE["⚛️ Admin UI Frontend<br/><i>Vite/React SPA :3000</i>"]
        BE["☕ Spring Boot Backend API<br/><i>Port :8080</i>"]
        DB["🐬 MySQL Database<br/><i>Port :3306</i>"]
        RD["🗄️ Redis Cache/Lock<br/><i>Port :6379</i>"]
        KF["🎛️ Apache Kafka Broker<br/><i>Port :9092/:9094</i>"]
        KUI["🖥️ Kafka-UI Web Console<br/><i>Port :8085</i>"]
    end

    C --> NG
    NG -->|"/"| FE
    NG -->|"/api/v1/*"| BE
    FE -->|"/api/v1/*"| BE
    BE --> DB
    BE --> RD
    BE --> KF
```

---

## 📊 Verified Project Metrics

```mermaid
xychart-beta
    title "System Verification Metrics"
    x-axis ["Test Files", "Flyway Migrations", "DB Tables", "REST Endpoints", "K6 Test Scripts"]
    y-axis "Count" 0 --> 45
    bar [27, 10, 15, 39, 8]
```

| Metric | Value | Status |
|--------|-------|--------|
| **Backend JUnit Test Files** | 27 (covering 75+ unit/integration assertions) | ✅ Verified |
| **Database Migrations** | 10 Flyway migration files | ✅ Up-to-date |
| **Relational Schema** | 15 tables (MySQL 8.4) | ✅ Validated |
| **REST API Endpoints** | 39 distinct endpoints | ✅ Implemented |
| **Frontend Test Files** | 20 unit/Playwright specification files | ✅ Verified |
| **K6 Benchmark Scenarios** | 8 load simulation scripts | ✅ Active |

---

## 🧠 Architectural Decision: Why Modular Monolith?

During initial planning phases, we evaluated whether to construct this system as a set of separate microservices (e.g. inventory-service, campaign-service, order-service). We chose a **Modular Monolith** architecture based on the following reasons:

1. **Transaction Boundaries**: Reserving inventory during flash sales requires atomic updates to inventory tables and campaign quota tables. Implementing this across network barriers requires 2PC or Saga patterns, which introduce significant latency and fail-modes under hot SKU contention. Keeping them in one database transaction ensures ACID reliability.
2. **Minimal Merge and Operational Friction**: Concurrency tuning and locking mechanisms are complex. Organizing them into discrete Java modules (`modules/*`) using Maven Reactor allows compile-time boundary enforcement, keeping modules clean and independent while running in a single deployable application (`apps/api`).
3. **Optimized Latency**: K6 benchmarks prove that local operations finish under **5.57ms** for outbox writes and **164.13ms** under extreme Hot SKU thread contention. In a microservices mesh, network hops between services would amplify lock-holding times, reducing overall throughput.

```mermaid
graph TD
    api["🔴 apps/api<br/>(Monolith Ingress Gateway)"]
    common["🟢 modules/common"]
    channel["🔵 modules/channel"]
    flashsale["🔵 modules/flashsale"]
    inventory["🔵 modules/inventory"]
    order["🔵 modules/order"]
    outbox["⚫ modules/outbox"]

    api --> channel
    api --> flashsale
    api --> inventory
    api --> order
    api --> outbox
    channel --> common
    flashsale --> common
    inventory --> common
    order --> common
    outbox --> common
    flashsale --> inventory
```

> ⬆️ **Dependency Flow: common ← modules ← apps/api** (Inner common utilities never depend on business modules).

---

## 📂 Project Structure

```
.
├── .github/
│   └── workflows/
│       ├── ci.yml              # Build, lint, and run Playwright + Maven tests
│       └── cd.yml              # SCP configuration and deploy via SSH compose
├── apps/
│   ├── api/                    # Spring Boot deployment application (Ingress Gateway)
│   └── admin-ui/               # Vite React Admin Dashboard SPA (Campaign & Ops Management)
├── modules/
│   ├── common/                 # Base classes, exception handlers, and standard wrappers
│   ├── channel/                # Omnichannel validation, connectors, sync schedulers
│   ├── flashsale/              # Campaigns management, active timers, quota rules
│   ├── inventory/              # Physical inventory logic, Redis & optimistic version locks
│   ├── order/                  # Order status transitions and state checking
│   └── outbox/                 # Transactional Outbox registry & Kafka event publishers
├── testing/
│   ├── k6/                     # 8 K6 load benchmark scripts (latency, contention, recovery)
│   └── contracts/              # JSON Schema contract definitions for Outbox event payloads
└── docs/
    ├── system-map.md           # Visual architecture map, invariants, and main flows
    ├── business-rules.md       # Central domain rules and consistency policies
    ├── data-model.md           # Schema details, tables, constraints, and indexes
    └── ... (Durable knowledge index stored under docs/00_index.md)
```

---

## 🚀 Quick Start

### Prerequisites
* Java 21 SDK & Maven (or `./mvnw` wrapper)
* Node.js 20+ & npm
* Docker & Docker Compose

### 1. Launch Infrastructure
Boot up MySQL, Redis, Kafka, and Kafka-UI services:
```bash
docker compose up -d
```

### 2. Start Backend Application
Run the deployable app module:
```bash
./mvnw spring-boot:run -pl apps/api
```
*API Swagger UI is available at:* `http://localhost:8080/swagger-ui.html`

### 3. Start React Admin Dashboard
Install dependencies and run the development server:
```bash
cd apps/admin-ui
npm install
npm run dev
```
*Admin Dashboard available at:* `http://localhost:3000`

### 4. Run Load Benchmarks (K6)
Verify system performance under simulated load:
```bash
k6 run ./testing/k6/hot-sku-contention.js
```

---

## 🧪 Testing & Quality

```bash
# Execute full backend test suite (Unit + Integration via Testcontainers)
# Note: Requires a running Docker daemon on host machine
./mvnw test

# Execute admin-ui frontend unit tests
cd apps/admin-ui && npm test

# Run frontend Playwright End-to-End browser verification
cd apps/admin-ui && npm run test:e2e
```

---

## 🔐 Concurrency & Reliability Safeguards

* **JPA Optimistic Versioning**: Every update to `inventory_item` evaluates the `@Version` field, automatically aborting updates if another thread committed first.
* **Leased Redis Locks**: `RedisLockManager` executes `SET NX` with a configured TTL (default 5s) and release token, ensuring locks are released even if a node crashes mid-transaction.
* **At-Least-Once Delivery**: The outbox scheduler retrieves PENDING events and marks them PUBLISHED only after receiving a Kafka Acknowledge signal. Unconfirmed events are retried.
* **Dual-Validation Idempotency**: Identifies duplicate requests by matching the API request signature alongside a unique UUID database constraint, rejecting concurrent duplicate payloads.

---

## ⚠️ Known Limitations

| Area | Current Status | Future Plan |
|------|---------------|-------------|
| **Testcontainers In CI** | Requires local Docker daemon; integration tests are skipped in GHA environments without Docker-in-Docker support | Configure a dedicated runner with Docker pre-installed for CI |
| **Real Shopee/TikTok Keys** | Current connectors run in MOCK mode unless valid marketplace signature keys are provided | Integrate real partner credentials through secure HashiCorp Vault storage |
| **Replays & Audit Size** | Audit logs write directly to MySQL; high traffic may bloat the `admin_activity_audit` table | Archive older audit events asynchronously to an elastic stack or S3 cold storage |

---

## 🔮 Future Improvements

* **Distributed Lock Fallback**: Implement database-level pessimistic locking (`SELECT FOR UPDATE`) as a backup lock mechanism if Redis loses connectivity.
* **Elastic Buffer Queue**: Add local buffering or rate limiting before processing high-volume webhook events from TikTok Shop.
* **Advanced Reconciliation Reporting**: Export PDF/Excel summaries detailing reconciliation drifts and resolution history directly from the React UI.

---

<div align="center">

**Built following clean Modular Monolith patterns, concurrency safety practices, and production-grade eventing.**

</div>
