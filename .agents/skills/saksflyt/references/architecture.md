# Saksflyt Architecture

## Table of Contents
1. [Overview](#overview)
2. [Component Diagram](#component-diagram)
3. [Core Components](#core-components)
4. [Execution Lifecycle](#execution-lifecycle)
5. [Transaction Boundaries](#transaction-boundaries)
6. [Threading Model](#threading-model)
7. [Event System](#event-system)

## Overview

The saksflyt saga pattern orchestrates complex, multi-step business processes asynchronously.
It provides:
- **Durability**: Saga state persisted to database
- **Observability**: Full audit trail via `sistFullførtSteg` and `hendelser`
- **Resilience**: Failed sagas can be restarted from last completed step
- **Concurrency Control**: Lock-based sequencing for related operations

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SERVICE LAYER                                   │
│   (e.g., IverksettVedtakService, JournalforingService)                      │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │ calls
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ProsessinstansService (saksflyt-api)                    │
│  • Creates Prosessinstans with type, behandling, data                       │
│  • Publishes ProsessinstansOpprettetEvent                                   │
│  • Entry point: opprettProsessinstans*() methods                            │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │ Spring ApplicationEvent
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│               ProsessinstansOpprettetListener (saksflyt)                     │
│  • BEFORE_COMMIT: Check låsReferanse, set PÅ_VENT if needed                 │
│  • AFTER_COMMIT: Call ProsessinstansBehandler.behandleProsessinstans()     │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │ @Async
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 ProsessinstansBehandler (saksflyt)                          │
│  • Finds ProsessFlyt for ProsessType                                        │
│  • Loops: nesteSteg() → hentStegBehandler() → utførSteg()                  │
│  • Updates sistFullførtSteg after each step                                 │
│  • Handles errors, sets status FEILET                                       │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │ for each step
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      StegBehandler implementations                          │
│  • Each step runs in REQUIRES_NEW transaction                               │
│  • Implements business logic (MEDL calls, brev sending, etc.)              │
│  • Located in saksflyt/src/main/java/.../steg/                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Core Components

### Prosessinstans (Entity)
**Location**: `saksflyt-api/src/main/kotlin/.../domain/Prosessinstans.kt`

The saga state holder, persisted to the `PROSESSINSTANS` table.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `type` | ProsessType | Which saga definition to use |
| `status` | ProsessStatus | Current state (KLAR, UNDER_BEHANDLING, etc.) |
| `behandling` | Behandling | Associated case/treatment (may be null) |
| `sistFullførtSteg` | ProsessSteg | Last successfully completed step |
| `data` | Properties | Key-value store for saga data |
| `hendelser` | List | Error events with stack traces |
| `låsReferanse` | String | Concurrency lock identifier |
| `registrertDato` | LocalDateTime | Creation timestamp |
| `endretDato` | LocalDateTime | Last modification timestamp |

### ProsessinstansService
**Location**: `saksflyt-api/src/main/java/.../ProsessinstansService.java`

Factory for creating sagas. Key methods:
- `opprettProsessinstansIverksettVedtakEos(Behandling)` - EØS verdict execution
- `opprettProsessinstansSedMottak(...)` - SED reception handling
- `opprettProsessinstansJournalføringNySak(...)` - Journal entry with new case

All methods:
1. Build a `Prosessinstans` with builder pattern
2. Set initial data using `ProsessDataKey`
3. Call `lagre()` which persists and publishes event

### ProsessinstansBehandler
**Location**: `saksflyt/src/main/java/.../ProsessinstansBehandler.java`

The saga orchestrator. Key method `behandleProsessinstans()`:

```java
@Async("saksflytThreadPoolTaskExecutor")
public void behandleProsessinstans(Prosessinstans prosessinstans) {
    // 1. Guard clauses for invalid states
    if (prosessinstans.erFerdig() || prosessinstans.erFeilet()) return;
    if (prosessinstans.erUnderBehandling()) return;

    // 2. Set UNDER_BEHANDLING
    prosessinstans.setStatus(UNDER_BEHANDLING);
    lagreProsessinstans(prosessinstans);

    // 3. Find flow definition
    ProsessflytDefinisjon.finnFlytForProsessType(prosessinstans.getType())
        .ifPresentOrElse(
            prosessFlyt -> this.utførFlyt(prosessinstans, prosessFlyt),
            () -> this.behandleFlytIkkeFunnet(prosessinstans)
        );
}
```

### ProsessFlyt & ProsessflytDefinisjon
**Location**: `saksflyt/src/main/kotlin/.../prosessflyt/`

`ProsessFlyt` holds an ordered list of `ProsessSteg`:
```kotlin
class ProsessFlyt(prosessType: ProsessType, vararg prosessSteg: ProsessSteg) {
    fun nesteSteg(forrigeSteg: ProsessSteg?): ProsessSteg?
}
```

`ProsessflytDefinisjon` maps `ProsessType` → `ProsessFlyt`:
```kotlin
object ProsessflytDefinisjon {
    private val PROSESS_FLYT_MAP: Map<ProsessType, ProsessFlyt> = mapOf(
        IVERKSETT_VEDTAK_EOS to ProsessFlyt(
            IVERKSETT_VEDTAK_EOS,
            AVKLAR_MYNDIGHET,
            AVKLAR_ARBEIDSGIVER,
            LAGRE_LOVVALGSPERIODE_MEDL,
            OPPRETT_FAKTURASERIE,
            SEND_VEDTAKSBREV_INNLAND,
            SEND_VEDTAK_UTLAND,
            DISTRIBUER_JOURNALPOST_UTLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        ),
        // ... 40+ more definitions
    )
}
```

### StegBehandler
**Location**: `saksflyt/src/main/java/.../steg/StegBehandler.java`

Interface for step implementations:
```java
public interface StegBehandler {
    ProsessSteg inngangsSteg();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void utfør(Prosessinstans prosessinstans);
}
```

Implementations are auto-registered via `@Component` and collected in constructor.

## Execution Lifecycle

```
1. Service calls ProsessinstansService.opprettProsessinstans*()
   └─> Prosessinstans created with status=KLAR
   └─> ProsessinstansOpprettetEvent published

2. ProsessinstansOpprettetListener (BEFORE_COMMIT phase)
   └─> Checks låsReferanse for conflicts
   └─> Sets status=PÅ_VENT if another saga has same lock group

3. ProsessinstansOpprettetListener (AFTER_COMMIT phase)
   └─> If not PÅ_VENT: calls behandleProsessinstans()

4. ProsessinstansBehandler.behandleProsessinstans() [@Async]
   └─> Sets status=UNDER_BEHANDLING
   └─> Loops through steps:
       └─> Get next step from ProsessFlyt
       └─> Get StegBehandler for step
       └─> Execute step (new transaction)
       └─> Update sistFullførtSteg
   └─> Sets status=FERDIG
   └─> Publishes ProsessinstansFerdigEvent

5. ProsessinstansFerdigListener
   └─> Finds waiting sagas with same lock group
   └─> Starts next in queue (oldest first)
```

## Transaction Boundaries

Critical for understanding race conditions:

| Phase | Transaction | Notes |
|-------|-------------|-------|
| Create Prosessinstans | Caller's TX | Persisted with initial state |
| Check låsReferanse | Caller's TX | BEFORE_COMMIT listener |
| Start execution | New TX per save | Each status update |
| Execute step | REQUIRES_NEW | Isolated step transaction |
| Handle error | Current TX | Rollback step, persist error |
| Complete saga | New TX | Final status update |

**Key insight 1**: The låsReferanse check happens in the SAME transaction as the creation.
This means concurrent creates can both pass the check before either commits.

**Key insight 2**: AFTER_COMMIT guarantees DB commit, but NOT fresh Java objects.
See [stale-reference-problem.md](stale-reference-problem.md) for details.

### The Stale Reference Problem

```java
// ProsessinstansOpprettetListener.java line 26-31
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
    Prosessinstans prosessinstans = event.hentProsessinstans();
    //                              ↑
    //     PROBLEM: Returns SAME Java object from HTTP session
    //     The 'behandling' inside is DETACHED with potentially STALE data
    //
    if (!prosessinstans.erPåVent()) {
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
    }
}
```

The saga receives the **same Prosessinstans object** that was created during the HTTP transaction.
Even though the DB is committed, the Java object graph contains **detached entities** that may have
stale data (data as it existed when loaded, not after all modifications in the HTTP transaction).

## Threading Model

```java
@Bean(name = "saksflytThreadPoolTaskExecutor")
public ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);  // 3 concurrent sagas
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(20);
    return executor;
}
```

- **Core pool**: 3 threads
- **Async execution**: `@Async("saksflytThreadPoolTaskExecutor")`
- **Queue**: Unlimited, but monitored via metrics
- **Graceful shutdown**: Waits up to 20 seconds

## Event System

### ProsessinstansOpprettetEvent
Published when saga is created. Listened to by `ProsessinstansOpprettetListener`:
- **BEFORE_COMMIT**: Lock check, may set PÅ_VENT
- **AFTER_COMMIT**: Starts async execution if not PÅ_VENT

### ProsessinstansFerdigEvent
Published when saga completes (FERDIG). Listened to by `ProsessinstansFerdigListener`:
- Finds waiting sagas with same lock group prefix
- Picks next by: sub-processes first, then siblings, then oldest
- Starts the selected saga

### Recovery on Startup
`ProsessinstansBehandler.gjenopprettProsesserSomHengerVedOppstart()`:
- Runs on `ApplicationReadyEvent`
- Finds active sagas unchanged for 24 hours
