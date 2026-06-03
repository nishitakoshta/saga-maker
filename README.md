# Saga-Maker

**Distributed Transaction Orchestrator for Microservices**

Saga-Maker is a lightweight Java library that implements the **Saga pattern** (orchestration-based) for managing distributed transactions across microservices. It handles step execution with exponential backoff retry, automatic compensation rollback on failure, and provides a built-in REST API to inspect saga state.

```
🏗️  Version: 1.0.0    ☕  Java 17+    📦  JitPack    🔌  Spring Boot 3.x
```

## Installation

Add JitPack repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.nishitakoshta</groupId>
    <artifactId>saga-maker</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or for Gradle:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.nishitakoshta:saga-maker:1.0.0'
}
```

---

## Table of Contents

- [Why Saga-Maker?](#why-saga-maker)
- [Architecture](#architecture)
- [Features](#features)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Annotation API](#annotation-api)
- [Error Handling &amp; Retry](#error-handling--retry)
- [Compensation Flow](#compensation-flow)
- [REST API (Built-in)](#rest-api-built-in)
- [Sample Application](#sample-application)
- [Comparison with Alternatives](#comparison-with-alternatives)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Why Saga-Maker?

### The Problem

In a microservices architecture, a single business operation often spans multiple services. For example, creating an order involves:

1. **Inventory Service** — reserve items
2. **Payment Service** — charge the user
3. **Order Service** — confirm the order

If step 2 fails, what happens to the inventory reserved in step 1? Without a saga, you end up with **inconsistent data** — reserved inventory that will never be purchased.

### The Solution: Saga Pattern

A saga is a sequence of local transactions where each step has a **compensation handler** that undoes its effect. If any step fails, the saga executes compensations for all completed steps in **reverse order**.

### Why This Library?

| Aspect | Saga-Maker | Other Solutions |
|--------|-----------|-----------------|
| **Weight** | Lightweight library (~50KB) | Heavy frameworks (Axon ~5MB, Eventuate ~15MB) |
| **Learning curve** | Minutes — define steps + compensations | Days — event sourcing, CQRS, aggregate setup |
| **External deps** | Zero — in-memory store works out of box | Requires databases, message brokers, event stores |
| **REST API** | Built-in `/_saga/instances` for inspection | Usually none — needs custom endpoints |
| **Spring Boot** | Auto-configuration, drop-in dependency | Manual configuration, XML/Java config |
| **Compensation** | Automatic — runs on any failure | Manual — you must handle rollback logic |
| **Retry** | Built-in exponential backoff | Requires custom implementation |

---

## Architecture

### High-Level Design

```
┌──────────────────────────────────────────────────────────────────┐
│                         Saga-Maker Library                         │
│                                                                    │
│  ┌──────────────┐        ┌──────────────────────┐                │
│  │   @Saga       │        │   SagaOrchestrator    │                │
│  │   @SagaStep   │───────▶│                      │                │
│  │   @Compensate │        │  ┌────────────────┐  │                │
│  └──────────────┘         │  │ executeSteps()  │  │                │
│                           │  │ withRetry()     │  │                │
│  ┌──────────────┐        │  │ compensate()    │  │                │
│  │  Programmatic │───────▶│  └────────────────┘  │                │
│  │  API          │        │                      │                │
│  └──────────────┘        └──────────┬───────────┘                │
│                                     │                             │
│                            ┌────────▼────────┐                    │
│                            │    SagaStore     │                    │
│                            │  (State Store)   │                    │
│                            │                  │                    │
│                            │  ┌────────────┐  │                    │
│                            │  │ InMemory   │  │                    │
│                            │  │ (Default)  │  │                    │
│                            │  └────────────┘  │                    │
│                            │  ┌────────────┐  │                    │
│                            │  │ JPAStore   │  │  (Roadmap)         │
│                            │  └────────────┘  │                    │
│                            │  ┌────────────┐  │                    │
│                            │  │ RedisStore │  │  (Roadmap)         │
│                            │  └────────────┘  │                    │
│                            └─────────────────┘                    │
│                                                                    │
│  ┌────────────────────────────────────────────┐                   │
│  │  Built-in REST Endpoint: /_saga/*          │                   │
│  │  - GET /_saga/instances                    │                   │
│  │  - GET /_saga/instances/{id}               │                   │
│  │  - GET /_saga/health                       │                   │
│  └────────────────────────────────────────────┘                   │
└──────────────────────────────────────────────────────────────────┘
```

### How It Works

```
   Client                     SagaOrchestrator           Service A           Service B
     │                              │                       │                   │
     │  startSaga()                 │                       │                   │
     │─────────────────────────────▶│                       │                   │
     │                              │                       │                   │
     │                              │──── Step 1 ──────────▶│                   │
     │                              │◀────── success ───────│                   │
     │                              │                       │                   │
     │                              │──── Step 2 ──────────────────────────────▶│
     │                              │◀────── FAIL ─────────────────────────────│
     │                              │                       │                   │
     │                              │◀── Compensate Step 1 ─│                   │
     │                              │◀── compensation done ─│                   │
     │                              │                       │                   │
     │  ◀──── SagaResult ────────│                       │                   │
     │  (COMPENSATED)               │                       │                   │
```

### Key Components

| Component | Responsibility |
|-----------|---------------|
| **SagaDefinition** | Defines a saga blueprint — its name, steps, and configuration |
| **SagaStepDefinition** | A single step with an action (`Consumer<SagaContext>`) and a compensation handler |
| **SagaContext** | Shared data bag that flows through all steps of a saga instance |
| **SagaInstance** | Represents a running/completed saga with its status and step execution history |
| **SagaOrchestrator** | Core engine — executes steps in order, handles retries, triggers compensation on failure |
| **SagaStore** | Persistence abstraction — stores saga definitions and instance state |

---

## Features

| Feature | Description | Status |
|---------|-------------|--------|
| ✅ Orchestration-based Saga | Central coordinator manages step execution | Done |
| ✅ Automatic Compensation | On failure, runs compensation handlers in reverse order | Done |
| ✅ Retry with Exponential Backoff | Configurable retries (1s, 2s, 4s, 8s... progression) | Done |
| ✅ State Management | In-memory store out of box, pluggable SPI for others | Done |
| ✅ Built-in REST API | Inspect saga state at `/_saga/instances` without any setup | Done |
| ✅ Spring Boot Auto-Config | Just add dependency, it works | Done |
| ✅ Annotation API | `@Saga`, `@SagaStep`, `@Compensate` for declarative sagas | Done |
| ✅ Programmatic API | Fluent builder for non-Spring or custom setups | Done |
| ✅ Zero External Dependencies | In-memory store means it works without any database | Done |
| ✅ Saga Context | Pass data between steps, store results per step | Done |
| 🔄 JPA Store | Persistent saga state across restarts | Roadmap |
| 🔄 Redis Store | Distributed saga state for clustered deployments | Roadmap |
| 🔄 Kafka/RabbitMQ Hooks | Emit events on step completion/failure | Roadmap |
| 🔄 WebSocket Dashboard | Live saga state monitoring | Roadmap |
| 🔄 Micrometer Metrics | Prometheus-compatible metrics | Roadmap |

---

## Quick Start

### 1. Define a saga (Programmatic API)

```java
// Define saga with 2 steps
SagaDefinition orderSaga = new SagaDefinition("order-creation")
    .setMaxRetries(3)
    .setRetryDelayMs(1000)
    .addStep(new SagaStepDefinition("reserve-inventory",
        // Action
        ctx -> {
            String itemId = ctx.get("itemId");
            Double qty = ctx.get("quantity");
            inventoryService.reserve(itemId, qty);
            ctx.putStepResult("reserve-inventory", "reserved:" + qty);
        },
        // Compensation (undo action)
        ctx -> {
            String itemId = ctx.get("itemId");
            Double qty = ctx.get("quantity");
            inventoryService.release(itemId, qty);
        }))
    .addStep(new SagaStepDefinition("process-payment",
        // Action
        ctx -> {
            paymentService.charge(ctx.get("userId"), ctx.get("amount"));
        },
        // Compensation
        ctx -> {
            paymentService.refund(ctx.get("userId"), ctx.get("amount"));
        }));

// Register saga
sagaStore.saveDefinition(orderSaga);
```

### 2. Execute the saga

```java
// Create context with input data
SagaContext context = new SagaContext("ORD-12345");
context.put("orderId", "ORD-12345");
context.put("userId", "user-1");
context.put("itemId", "item-1");
context.put("quantity", 2.0);
context.put("amount", 500.0);

// Execute saga
SagaInstance instance = orchestrator.startSaga("order-creation", context);

// Check result
if (instance.getStatus() == SagaStatus.COMPLETED) {
    log.info("Order created successfully!");
} else if (instance.getStatus() == SagaStatus.COMPENSATED) {
    log.warn("Order failed, all changes rolled back. Reason: {}",
        instance.getFailureReason());
}
```

### 3. Inspect saga state

```bash
# List all sagas
curl http://localhost:8081/_saga/instances

# Get specific saga by ID
curl http://localhost:8081/_saga/instances/{sagaInstanceId}

# Health check
curl http://localhost:8081/_saga/health
```

---

## API Reference

### SagaDefinition

Creates a saga blueprint.

```java
SagaDefinition def = new SagaDefinition("saga-name")
    .setMaxRetries(3)           // Default: 3
    .setRetryDelayMs(1000)      // Default: 1000ms
    .addStep(step1)             // Add steps in order
    .addStep(step2)
    .addStep(step3);
```

| Method | Description |
|--------|-------------|
| `SagaDefinition(String name)` | Create saga with a unique name |
| `addStep(SagaStepDefinition step)` | Add a step (steps execute in addition order) |
| `setMaxRetries(int maxRetries)` | Max retry attempts per step (default: 3) |
| `setRetryDelayMs(long retryDelayMs)` | Base delay for exponential backoff (default: 1000ms) |

### SagaStepDefinition

Defines a single step with action and compensation.

```java
SagaStepDefinition step = new SagaStepDefinition(
    "step-name",
    context -> {
        // Action logic — runs as part of the saga
        // Throw RuntimeException to trigger retry/compensation
    },
    context -> {
        // Compensation logic — runs if any later step fails
        // Should undo what the action did
    }
);
```

| Method | Description |
|--------|-------------|
| `SagaStepDefinition(String name, Consumer<SagaContext> action, Consumer<SagaContext> compensation)` | Create step |

### SagaContext

Thread-safe context for passing data between saga steps.

```java
SagaContext ctx = new SagaContext("order-123");

// Put input data
ctx.put("userId", "user-1");
ctx.put("amount", 500.0);

// Get input data in any step
String userId = ctx.get("userId");

// Store step results
ctx.putStepResult("reserve-inventory", reservedQty);
ctx.putStepResult("process-payment", transactionId);

// Retrieve step results in later steps
Double reservedQty = ctx.getStepResult("reserve-inventory");
```

| Method | Description |
|--------|-------------|
| `SagaContext(String sagaId)` | Create context with unique saga ID |
| `put(String key, Object value)` | Store data in context |
| `<T> get(String key)` | Retrieve data from context |
| `putStepResult(String stepName, Object result)` | Store step execution result |
| `<T> getStepResult(String stepName)` | Retrieve step execution result |
| `getSagaId()` | Get the saga business ID |

### SagaOrchestrator

Core engine that executes sagas.

```java
SagaOrchestrator orchestrator = new SagaOrchestrator(sagaStore);

// Execute saga
SagaInstance instance = orchestrator.startSaga("order-creation", context);

// Get status of any saga
SagaInstance status = orchestrator.getSagaStatus(instanceId);
```

| Method | Description |
|--------|-------------|
| `startSaga(String sagaName, SagaContext context)` | Execute a saga. Returns the completed SagaInstance |
| `getSagaStatus(String instanceId)` | Get the current state of a saga instance |

### SagaInstance

Represents a single execution of a saga.

```java
SagaInstance instance = orchestrator.startSaga("order-creation", context);

instance.getId();                    // Unique instance ID (UUID)
instance.getSagaName();              // Saga definition name
instance.getStatus();                // Current status
instance.getStepExecutions();        // List of step execution details
instance.getFailureReason();         // Reason if failed/compensated
instance.getRetryCount();            // Total retries attempted
instance.getCreatedAt();             // Timestamp saga started
instance.getCompletedAt();           // Timestamp saga completed
```

### Status Lifecycle

```
CREATED ──▶ STARTED ──▶ COMPLETED  (success)
                │
                └──▶ FAILED ──▶ COMPENSATING ──▶ COMPENSATED
                                      │
                                      └──▶ COMPENSATION_FAILED
```

| Status | Meaning |
|--------|---------|
| `CREATED` | Saga instance created but not started |
| `STARTED` | Saga execution is in progress |
| `COMPLETED` | All steps executed successfully |
| `FAILED` | A step failed beyond max retries |
| `COMPENSATING` | Running compensation handlers in reverse |
| `COMPENSATED` | All compensations completed successfully |
| `COMPENSATION_FAILED` | A compensation handler threw an exception |

---

## Configuration

### Default Configuration

```java
// In-memory saga store (default)
SagaStore store = new InMemorySagaStore();
SagaOrchestrator orchestrator = new SagaOrchestrator(store);
```

### Spring Boot Auto-Configuration

When using Spring Boot, SagaStore and SagaOrchestrator beans are auto-created:

```yaml
# application.yml — no config needed, works out of box
# SagaEndpoint is auto-registered at /_saga/*
```

### Custom SagaStore

Implement `SagaStore` to persist saga state:

```java
public class CustomSagaStore implements SagaStore {
    @Override
    public void saveDefinition(SagaDefinition definition) { /* ... */ }

    @Override
    public Optional<SagaDefinition> findDefinition(String name) { /* ... */ }

    @Override
    public void saveInstance(SagaInstance instance) { /* ... */ }

    @Override
    public Optional<SagaInstance> findInstance(String id) { /* ... */ }

    @Override
    public List<SagaInstance> findAllInstances() { /* ... */ }

    @Override
    public List<SagaInstance> findInstancesByStatus(String sagaName,
                                                     SagaStatus status) { /* ... */ }
}
```

Register as a bean:

```java
@Bean
public SagaStore sagaStore() {
    return new CustomSagaStore();
}
```

---

## Annotation API

Saga-Maker provides annotations for declarative saga definition. This is ideal when your step logic is in separate Spring beans.

### Step 1: Define saga with annotations

```java
@Component
@Saga(name = "order-creation", maxRetries = 3, retryDelayMs = 1000)
public class OrderSaga {

    @SagaStep(name = "reserve-inventory", order = 1)
    public void reserveInventory(SagaContext ctx) {
        String itemId = ctx.get("itemId");
        Double qty = ctx.get("quantity");
        inventoryService.reserve(itemId, qty);
        ctx.putStepResult("reserve-inventory", "done");
    }

    @Compensate(stepName = "reserve-inventory")
    public void compensateReserveInventory(SagaContext ctx) {
        String itemId = ctx.get("itemId");
        Double qty = ctx.get("quantity");
        inventoryService.release(itemId, qty);
    }

    @SagaStep(name = "process-payment", order = 2)
    public void processPayment(SagaContext ctx) {
        paymentService.charge(ctx.get("userId"), ctx.get("amount"));
    }

    @Compensate(stepName = "process-payment")
    public void compensatePayment(SagaContext ctx) {
        paymentService.refund(ctx.get("userId"), ctx.get("amount"));
    }
}
```

### Step 2: Execute

```java
SagaContext ctx = new SagaContext("ORD-123");
ctx.put("userId", "user-1");
// ... more data

SagaInstance instance = orchestrator.startSaga("order-creation", ctx);
```

### Annotation Reference

| Annotation | Target | Description |
|------------|--------|-------------|
| `@Saga` | Class | Marks a class as a saga definition. Parameters: `name`, `maxRetries`, `retryDelayMs` |
| `@SagaStep` | Method | Marks a method as a saga step. Parameters: `name`, `order` |
| `@Compensate` | Method | Marks a method as compensation for a step. Parameter: `stepName` |

---

## Error Handling & Retry

### Retry Behavior

When a step action throws a `RuntimeException`, Saga-Maker retries with **exponential backoff**:

```
Attempt 1: wait 1000ms  (base delay)
Attempt 2: wait 2000ms  (base × 2)
Attempt 3: wait 4000ms  (base × 4)
Attempt 4: wait 8000ms  (base × 8) — final attempt
```

On failure of all attempts:
1. The saga status changes to `FAILED`
2. Compensation is triggered for all previously completed steps

### Configuring Retry

```java
// Per saga
SagaDefinition saga = new SagaDefinition("my-saga")
    .setMaxRetries(5)        // Total attempts = maxRetries + 1
    .setRetryDelayMs(500);   // Exponential base in ms
```

### Handling Failures in Code

```java
SagaInstance instance = orchestrator.startSaga("order-creation", context);

switch (instance.getStatus()) {
    case COMPLETED:
        // All good
        break;

    case COMPENSATED:
        log.error("Saga failed and compensated. Reason: {}",
            instance.getFailureReason());
        // All changes have been rolled back
        break;

    case COMPENSATION_FAILED:
        log.error("CRITICAL: Saga failed and compensation also failed! " +
            "Manual intervention required. Reason: {}",
            instance.getFailureReason());
        // Some compensations completed, some didn't
        // Check individual step statuses
        break;
}
```

### Best Practices

1. **Idempotency** — Ensure actions and compensations are idempotent. If a step succeeds but the orchestrator doesn't receive the confirmation, it may retry. The action should handle this safely.

2. **Fail Fast** — Validate inputs before starting a saga. Fail early with meaningful messages.

3. **Compensation Completeness** — Every action must have a corresponding compensation. If a compensation fails, manual intervention may be needed.

4. **Logging** — Always log in actions and compensations. Saga-Maker logs automatically, but your business logic should too.

---

## Compensation Flow

### Failure Scenario

When `process-payment` fails after `reserve-inventory` completed:

```
Saga: order-creation
  ├── Step 1: reserve-inventory  ✅  SUCCESS
  ├── Step 2: process-payment     ❌  FAIL (retries exhausted)
  │
  └── Compensation (reverse order):
       └── Step 1: reserve-inventory  🔄  RELEASED
```

### Detailed State During Compensation

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sagaName": "order-creation",
  "status": "COMPENSATED",
  "failureReason": "Step 'process-payment' failed after 4 attempts",
  "stepExecutions": [
    {
      "stepName": "reserve-inventory",
      "status": "COMPENSATED",
      "startedAt": 1700000000000,
      "completedAt": 1700000000100,
      "retryCount": 0
    },
    {
      "stepName": "process-payment",
      "status": "FAILED",
      "startedAt": 1700000000100,
      "errorMessage": "Insufficient balance...",
      "retryCount": 3
    }
  ]
}
```

---

## REST API (Built-in)

When running within Spring Boot, Saga-Maker automatically exposes:

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/_saga/instances` | List all saga instances |
| `GET` | `/_saga/instances/{id}` | Get specific saga instance details |
| `GET` | `/_saga/health` | Health check with saga count |

### List all sagas

```bash
curl http://localhost:8081/_saga/instances
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "sagaName": "order-creation",
    "status": "COMPLETED",
    "failureReason": null,
    "retryCount": 0,
    "createdAt": 1700000000000,
    "completedAt": 1700000000500,
    "stepExecutions": [
      {
        "stepName": "reserve-inventory",
        "status": "COMPLETED",
        "startedAt": 1700000000000,
        "completedAt": 1700000000100,
        "retryCount": 0
      },
      {
        "stepName": "process-payment",
        "status": "COMPLETED",
        "startedAt": 1700000000100,
        "completedAt": 1700000000200,
        "retryCount": 0
      }
    ]
  }
]
```

### Get specific saga

```bash
curl http://localhost:8081/_saga/instances/550e8400-e29b-41d4-a716-446655440000
```

### Health check

```bash
curl http://localhost:8081/_saga/health
```

Response:
```json
{
  "status": "UP",
  "sagaCount": "42"
}
```

---

## Sample Application

The `saga-maker-sample` module contains a fully working **Order Service** that demonstrates Saga-Maker in action.

### What it does

Creates an order by orchestrating 3 services:

1. **Reserve Inventory** — deducts quantity from stock
2. **Process Payment** — charges the user's balance
3. **Confirm Order** — marks the order as complete

If any step fails, all completed steps are automatically compensated.

### Pre-loaded Data

| Item | Stock |
|------|-------|
| item-1 | 100 units |
| item-2 | 50 units |

| User | Balance |
|------|---------|
| user-1 | ₹5000 |

### Run the sample

```bash
# From project root
cd saga-maker-sample
mvn spring-boot:run
```

The app starts on http://localhost:8081

### Test 1: Successful Order

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","itemId":"item-1","quantity":2,"amount":500}'
```

Expected response:
```json
{
  "orderId": "ORD-1A2B3C4D",
  "sagaId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "failureReason": ""
}
```

Verify state:
```bash
curl http://localhost:8081/_saga/instances
# -> status shows COMPLETED, all 3 steps COMPLETED
```

### Test 2: Failed Order (Insufficient Inventory)

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","itemId":"item-1","quantity":999,"amount":500}'
```

Expected response:
```json
{
  "orderId": "ORD-9Z8Y7X6W",
  "sagaId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "COMPENSATED",
  "failureReason": "Step 'reserve-inventory' failed after 4 attempts: Insufficient inventory..."
}
```

Since no steps completed, **no compensation was needed**.

### Test 3: Failed Order (Insufficient Balance)

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","itemId":"item-1","quantity":2,"amount":99999}'
```

Expected response:
```json
{
  "orderId": "ORD-5A4B3C2D",
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "status": "COMPENSATED",
  "failureReason": "Step 'process-payment' failed after 4 attempts: Insufficient balance..."
}
```

Check compensation:
```bash
curl http://localhost:8081/_saga/instances/770e8400-e29b-41d4-a716-446655440002
# -> reserve-inventory: COMPENSATED (inventory was released)
# -> process-payment: FAILED
```

### Demo Output Walkthrough

```
2026-06-02 INFO  --- Registered saga: order-creation (3 steps)

--- Test 1: Success ---
2026-06-02 INFO  --- Reserved 2 units of item-1 (remaining: 98)
2026-06-02 INFO  --- Charged 500 from user user-1 (remaining: 4500)
2026-06-02 INFO  --- Order ORD-1A2B3C4D confirmed
2026-06-02 INFO  --- Saga [order-creation] instance [uuid] completed successfully

--- Test 3: Failure + Compensation ---
2026-06-02 INFO  --- Reserved 2 units of item-1 (remaining: 96)
2026-06-02 WARN  --- Step [process-payment] failed (attempt 1/4): Insufficient balance...
2026-06-02 WARN  --- Step [process-payment] failed (attempt 2/4): Insufficient balance...
2026-06-02 WARN  --- Step [process-payment] failed (attempt 3/4): Insufficient balance...
2026-06-02 WARN  --- Step [process-payment] failed (attempt 4/4): Insufficient balance...
2026-06-02 INFO  --- Starting compensation for saga [order-creation] instance [uuid]
2026-06-02 INFO  --- Compensation for step [reserve-inventory] completed
2026-06-02 INFO  --- Compensation completed for saga [order-creation] instance [uuid]
```

---

## Comparison with Alternatives

| Feature | Saga-Maker | Axon Framework | Eventuate Tram | Custom Event-Driven |
|---------|-----------|---------------|----------------|-------------------|
| **Pattern** | Orchestration Saga | Event Sourcing + CQRS | Saga + Kafka | Manual |
| **Learning Curve** | ⭐ (minutes) | ⭐⭐⭐⭐⭐ (weeks) | ⭐⭐⭐ (days) | ⭐⭐⭐⭐ (varies) |
| **Setup Time** | ⭐ (5 min) | ⭐⭐⭐⭐⭐ (days) | ⭐⭐⭐ (hours) | ⭐⭐⭐⭐ (days) |
| **Boilerplate** | Minimal — 2 lambdas per step | Extensive — aggregates, events, sagas, handlers | Moderate — channels, subscriptions | Full implementation |
| **State Store** | In-memory/pluggable | Event Store required | Database required | Depends on implementation |
| **Runtime Inspection** | Built-in REST API | Axon Dashboard | Manual | Manual |
| **Retry** | Built-in with backoff | Manual | Manual | Manual |
| **Compensation** | Automatic | Manual (saga handler) | Automatic | Manual |
| **Dependency Size** | ~50KB | ~5MB+ | ~2MB+ | Variable |
| **External Requirements** | Zero | Event Store required | Kafka + Database | Variable |
| **Ideal For** | Quick integration, microservices, startups | Large-scale event sourcing systems | Kafka-based systems | Custom architectures |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.x (optional — core works standalone) |
| **Build** | Maven |
| **Persistence** | In-memory (default), JPA/Redis (pluggable) |
| **Testing** | JUnit 5 |
| **Logging** | SLF4J |

---

## Project Structure

```
saga-maker/
├── pom.xml                              # Parent POM (multi-module)
├── README.md                            # This file
│
├── saga-maker-core/                     # Core library module
│   ├── pom.xml
│   └── src/main/java/com/sagamaker/core/
│       ├── annotation/                  # @Saga, @SagaStep, @Compensate
│       │   ├── Saga.java
│       │   ├── SagaStep.java
│       │   └── Compensate.java
│       ├── model/                       # Core domain models
│       │   ├── SagaStatus.java          # Saga lifecycle states
│       │   ├── StepStatus.java          # Step execution states
│       │   ├── SagaContext.java         # Shared context between steps
│       │   ├── SagaDefinition.java      # Saga blueprint
│       │   ├── SagaStepDefinition.java  # Step blueprint with action + compensation
│       │   ├── SagaInstance.java        # Runtime saga instance
│       │   └── SagaStepExecution.java   # Runtime step execution record
│       ├── engine/
│       │   └── SagaOrchestrator.java    # Core execution engine
│       ├── store/
│       │   ├── SagaStore.java           # Store interface (pluggable)
│       │   └── InMemorySagaStore.java   # Default in-memory implementation
│       ├── endpoint/
│       │   └── SagaEndpoint.java        # Built-in REST endpoints
│       ├── config/
│       │   └── SagaMakerAutoConfiguration.java  # Spring Boot auto-config
│       └── exception/
│           ├── SagaExecutionException.java
│           └── CompensationException.java
│
└── saga-maker-sample/                   # Demo application
    ├── pom.xml
    └── src/main/java/com/sagamaker/sample/
        ├── SagaSampleApplication.java   # Spring Boot app with saga definition
        ├── controller/
        │   └── OrderController.java     # REST controller to trigger orders
        └── dto/
            └── OrderRequest.java        # Request DTO
```

---

## Roadmap

### Short Term (Q2 2026)
- [ ] JPA-backed saga store for persistence across restarts
- [ ] Redis-backed saga store for distributed deployments
- [ ] OpenAPI/Swagger docs for `/_saga` endpoints

### Medium Term (Q3 2026)
- [ ] Kafka/RabbitMQ event hooks — emit events on step completion/failure
- [ ] WebSocket dashboard for live saga state monitoring
- [ ] Micrometer metrics for Prometheus/Grafana

### Long Term
- [ ] Choreography-based saga support (event-driven)
- [ ] Saga visualizer — generate sequence diagrams from saga definitions
- [ ] Maven Central release

---

## Contributing

Contributions are welcome! Here's how you can help:

1. **Report bugs** — Open an issue with a clear description
2. **Suggest features** — Open an issue with use cases
3. **Submit PRs** — Fork, create a feature branch, and submit a PR
4. **Improve docs** — Better examples, clearer explanations

### Development Setup

```bash
# Clone
git clone https://github.com/nishitakoshta/saga-maker.git
cd saga-maker

# Build
mvn clean install -DskipTests

# Run tests
mvn test

# Run sample app
cd saga-maker-sample
mvn spring-boot:run
```
### Demo - https://github.com/nishitakoshta/saga-maker-travel-demo
---

## License

MIT License — free to use, modify, and distribute.

---

## Author

**Nishita Koshta** — Java Backend Developer

- [GitHub](https://github.com/nishitakoshta)
- [LinkedIn](https://linkedin.com/in/nishita-koshta)
