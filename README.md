# Trading API

A backtest execution service for trading strategies, built with Kotlin and Spring Boot following Clean Architecture principles. The API lets clients create backtests, track their lifecycle through a well-defined state machine, cancel running jobs, and retry failed ones — all backed by asynchronous processing via Kotlin Coroutines.

This project is intentionally small in scope but opinionated in structure: it demonstrates how to keep business rules isolated from frameworks, how to model state transitions as first-class domain concepts, and how to compose use cases cleanly in a Spring Boot application.

---

## Live Demo

The API is deployed and publicly available at:

```
https://trading-api-6irv.onrender.com
```

> **Note:** The app runs on Render's free tier, which hibernates after 15 minutes of inactivity. The first request after hibernation may take 30-60 seconds while the container spins back up. Subsequent requests respond normally.

Quick test:

```bash
# List all backtests
curl https://trading-api-6irv.onrender.com/backtests

# Create a backtest
curl -X POST https://trading-api-6irv.onrender.com/backtests \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Test","startDate":"2025-01-01","endDate":"2025-12-31"}'
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1.10 |
| Runtime | Java 17 |
| Framework | Spring Boot 3.4.3 (Web, Data JPA) |
| Async | Kotlin Coroutines |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Build | Gradle (Kotlin DSL) |
| Testing | JUnit 5, Kotlin Test |
| Infra | Docker Compose |

---

## Architecture

The project follows **Clean Architecture** with four layers. Dependencies point inward: outer layers depend on inner layers, never the reverse.

```
┌──────────────────────────────────────────────────────────┐
│                         api/                            │
│  REST Controllers, DTOs, Exception Handlers              │
│  (BacktestController, ApiExceptionHandler)               │
└────────────────────────┬─────────────────────────────────┘
                         │ depends on
                         ▼
┌──────────────────────────────────────────────────────────┐
│                     application/                        │
│  Use Cases — orchestration of domain rules               │
│  (CreateBacktestUseCase, ExecuteBacktestUseCase, ...)    │
└────────────────────────┬─────────────────────────────────┘
                         │ depends on
                         ▼
┌──────────────────────────────────────────────────────────┐
│                       domain/                           │
│  Entities, Value Objects, Repository interfaces,         │
│  State machine rules — pure Kotlin, no frameworks        │
│  (Backtest, BacktestStatus, BacktestRepository)          │
└──────────────────────────────────────────────────────────┘
                         ▲
                         │ implements
                         │
┌────────────────────────┴─────────────────────────────────┐
│                        infra/                           │
│  JPA entities, Spring Data repositories,                 │
│  Repository implementations (adapters)                   │
│  (BacktestEntity, JpaBacktestRepository, ...Impl)        │
└──────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

- **`domain/`** — Business entities and rules. Zero dependencies on Spring, JPA, or any framework. Defines `BacktestRepository` as an interface (dependency inversion).
- **`application/`** — Use cases that orchestrate domain objects to fulfill a single business operation. Each use case is a `@Service` with one public `execute()` method.
- **`api/`** — HTTP layer. Controllers map HTTP requests to use case inputs, DTOs isolate the wire format from the domain, and a global exception handler maps domain exceptions to HTTP status codes.
- **`infra/`** — Framework-specific adapters. JPA entities, Spring Data repositories, and the concrete implementation of the domain's `BacktestRepository` interface.

---

## API Endpoints

Base URL: `http://localhost:8080`

### Create Backtest

```
POST /backtests
```

**Request Body**
```json
{
  "name": "BTC Moving Average Crossover",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31"
}
```

**Response** — `202 Accepted`
```json
{
  "id": "3f8f9d8d-a18e-4038-ae3c-d4e299117009",
  "status": "PENDING"
}
```

Backtest execution is triggered asynchronously. The client should poll `GET /backtests/{id}` to observe status changes.

---

### List Backtests

```
GET /backtests
```

**Response** — `200 OK`
```json
[
  {
    "id": "3f8f9d8d-a18e-4038-ae3c-d4e299117009",
    "name": "BTC Moving Average Crossover",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31",
    "status": "COMPLETED",
    "result": { "pnl": 1542.37 },
    "createdAt": "2026-04-02T22:41:25.678991Z"
  }
]
```

---

### Get Backtest by ID

```
GET /backtests/{id}
```

**Response** — `200 OK`
```json
{
  "id": "3f8f9d8d-a18e-4038-ae3c-d4e299117009",
  "name": "BTC Moving Average Crossover",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "status": "RUNNING",
  "result": null,
  "createdAt": "2026-04-02T22:41:25.678991Z"
}
```

**Error** — `404 Not Found`
```json
{ "message": "Backtest {id} not found" }
```

---

### Cancel Backtest

```
DELETE /backtests/{id}
```

Cancels a backtest that is currently `RUNNING`.

**Response** — `200 OK`
```json
{
  "id": "3f8f9d8d-a18e-4038-ae3c-d4e299117009",
  "name": "BTC Moving Average Crossover",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "status": "CANCELED",
  "result": null,
  "createdAt": "2026-04-02T22:41:25.678991Z"
}
```

**Error** — `409 Conflict` (invalid state transition)
```json
{ "message": "Cannot transition from PENDING to CANCELED" }
```

---

### Retry Failed Backtest

```
POST /backtests/{id}/retry
```

Re-runs a backtest in the `FAILED` state. Transitions `FAILED → RUNNING` and dispatches execution in the background.

**Response** — `202 Accepted`
```json
{
  "id": "3f8f9d8d-a18e-4038-ae3c-d4e299117009",
  "name": "BTC Moving Average Crossover",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "status": "RUNNING",
  "result": null,
  "createdAt": "2026-04-02T22:41:25.678991Z"
}
```

**Error** — `409 Conflict` if the current status is not `FAILED`.

---

## Backtest State Machine

The `Backtest` entity has an explicit state machine enforced inside the domain layer. Invalid transitions throw `IllegalStateException`, which the API layer maps to `409 Conflict`.

```
                  ┌─────────┐
                  │ PENDING │
                  └────┬────┘
                       │
                       ▼
                  ┌─────────┐
        ┌────────▶│ RUNNING │◀────────┐
        │         └────┬────┘         │
        │              │              │
        │      ┌───────┼───────┐      │
        │      ▼       ▼       ▼      │
        │  ┌───────┐ ┌────┐ ┌──────┐  │
        │  │COMPLE-│ │FAIL│ │CANCE-│  │
        │  │ TED   │ │ ED │ │ LED  │  │
        │  └───────┘ └──┬─┘ └──────┘  │
        │   (terminal)  │  (terminal) │
        │               │             │
        │               └─────────────┘
        │                   retry
        │
        └── COMPLETED and CANCELED are terminal
```

### Allowed Transitions

| From | To | Trigger |
|---|---|---|
| `PENDING` | `RUNNING` | Execution dispatched |
| `RUNNING` | `COMPLETED` | Execution finished successfully |
| `RUNNING` | `FAILED` | Execution threw an exception |
| `RUNNING` | `CANCELED` | `DELETE /backtests/{id}` |
| `FAILED` | `RUNNING` | `POST /backtests/{id}/retry` |

`COMPLETED` and `CANCELED` are terminal — no further transitions allowed.

---

## Running Locally

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Gradle (the wrapper is included, so no local install required)

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a PostgreSQL 16 container on port `5432` with:
- Database: `trading`
- User: `trading`
- Password: `trading`

### 2. Run the Application

```bash
./gradlew bootRun
```

Flyway applies migrations automatically on startup. The API will be available at `http://localhost:8080`.

### 3. Quick Smoke Test

```bash
curl -X POST http://localhost:8080/backtests \
  -H 'Content-Type: application/json' \
  -d '{"name":"Smoke Test","startDate":"2025-01-01","endDate":"2025-12-31"}'
```

---

## Running Tests

Run the full test suite:

```bash
./gradlew test
```

Run only the domain unit tests (no database required, fast):

```bash
./gradlew test --tests "com.trading.domain.BacktestTest"
```

The domain tests cover every valid and invalid state transition in the `BacktestStatus` state machine. They are pure Kotlin tests and do not boot the Spring context.

---

## Technical Decisions

### Why Clean Architecture?

The domain layer has zero framework dependencies. This means:

- **Business rules are testable in isolation** — no need to boot Spring or spin up a database to test a state transition.
- **Frameworks are replaceable** — swapping JPA for jOOQ, or Postgres for another store, requires changes only in `infra/`.
- **Intent is explicit** — reading `application/` tells you *what the system does*, without noise from persistence or HTTP concerns.

The tradeoff is slightly more boilerplate (mapping between `BacktestEntity` and `Backtest`), which is a reasonable price for the isolation.

### Why State Machine Rules in the Domain?

`BacktestStatus.canTransitionTo()` and `Backtest.transitionTo()` live in the domain because state transitions *are* the core business rule. Putting them in a service or controller would let invalid states leak into the system. By making transitions the only way to change status — and having them throw on invalid moves — the domain is correct by construction. No one can accidentally save a `Backtest` that went `PENDING → COMPLETED` directly.

### Why Kotlin Coroutines for Async Execution?

Backtests simulate a long-running computation (5-second delay in the current implementation, real CPU/IO work later). Blocking a request thread for the duration of the computation would waste resources and degrade throughput.

Coroutines were chosen over `@Async` or a `TaskExecutor` because:

- **Idiomatic in Kotlin** — `launch`, `delay`, and structured concurrency read naturally.
- **Lightweight** — thousands of coroutines can coexist on a small thread pool.
- **Explicit scope** — `CoroutineScope(Dispatchers.Default + SupervisorJob())` makes failure isolation visible in the code, not hidden in framework config.

### Why `Dispatchers.Default`?

`ExecuteBacktestUseCase` runs on `Dispatchers.Default`, which is backed by a thread pool sized to the number of CPU cores. This is the right choice because backtests are expected to be **CPU-bound** (running strategy logic over historical data).

If execution were I/O-bound (e.g., fetching market data over HTTP), `Dispatchers.IO` would be more appropriate — it has a larger pool optimized for blocking calls. The choice of dispatcher is a deliberate signal about the workload's nature.

`SupervisorJob` ensures that one failed backtest does not cancel sibling coroutines — failures are isolated at the task level.

---

## Next Steps

Planned improvements, in rough priority order:

### Authentication — JWT
- Add Spring Security with JWT bearer tokens.
- Scope backtests per user (`ownerId` on the entity).
- Role-based access: regular users can manage their own backtests, admins can list all.

### Event-Driven Execution — Kafka
- Replace the in-process coroutine dispatch with a Kafka topic (`backtest.requested`).
- A separate worker service consumes the topic and executes backtests.
- Benefits: horizontal scaling of workers, durability across restarts, back-pressure handling, and recovery of orphaned `RUNNING` jobs.

### Integration Tests
- Add `@SpringBootTest` integration tests using **Testcontainers** for a real PostgreSQL instance — no mocking the database.
- Cover the full HTTP → use case → repository path for each endpoint.
- Add a contract test suite that exercises the state machine end-to-end (create → run → complete, create → cancel, etc.).

### Other Improvements
- Pagination and filtering on `GET /backtests`.
- Structured error responses with error codes (not just messages).
- Observability: Micrometer metrics, structured logging, OpenTelemetry traces.
- OpenAPI / Swagger UI for interactive API documentation.
- Idempotency keys on `POST /backtests` to avoid duplicate submissions.
