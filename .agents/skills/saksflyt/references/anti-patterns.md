# Saksflyt Anti-Patterns

## Table of Contents
1. [Critical: The AFTER_COMMIT Misconception](#critical-the-after_commit-misconception)
2. [The Sync/Async Split Problem](#the-syncasync-split-problem)
3. [Anti-Pattern: Synchronous Event Listeners Modifying Entities](#anti-pattern-synchronous-event-listeners-modifying-entities)
4. [Anti-Pattern: Loading Entities Across Transaction Boundaries](#anti-pattern-loading-entities-across-transaction-boundaries)
5. [Solutions and Mitigations](#solutions-and-mitigations)
6. [Decision Framework](#decision-framework)

---

## Critical: The AFTER_COMMIT Misconception

> **Common misconception**: "The saga starts AFTER_COMMIT, so it sees committed data."
>
> **Reality**: AFTER_COMMIT guarantees DB commit, NOT fresh Java objects in the saga.

See **[stale-reference-problem.md](stale-reference-problem.md)** for the full analysis.

### Quick Summary

The `ProsessinstansOpprettetListener` (line 26-31) uses `@TransactionalEventListener(phase = AFTER_COMMIT)`:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
    Prosessinstans prosessinstans = event.hentProsessinstans();  // ← SAME Java object!
    // ...
}
```

**The problem**: `event.hentProsessinstans()` returns the **same Java object** from the HTTP transaction, not a fresh load. The `behandling` reference inside is a **detached entity** with potentially stale data.

### Three Flavors of Race Conditions

| Flavor | Symptom | Mechanism |
|--------|---------|-----------|
| **Stale Object Reference** | Silent corruption or StaleObjectStateException | Event passes detached entity graph |
| **Sync/Async Conflict** | OptimisticLockingException | Listener + saga step modify same entity |
| **Cross-TX Loading** | Silent corruption | Saga loads other behandling's entities |

### Recommended Fix

Reload `Prosessinstans` fresh from DB in AFTER_COMMIT listener. See [stale-reference-problem.md](stale-reference-problem.md).

---

## The Sync/Async Split Problem

The saksflyt architecture creates a **transaction boundary split** that is the root cause of
multiple race conditions:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST TRANSACTION                             │
│                                                                              │
│  fattVedtak() {                                                              │
│    behandling.setStatus(IVERKSETTER_VEDTAK)  ─┬─ Triggers BehandlingEndretStatusEvent
│    behandlingsresultat.setType(MEDLEM)        │                              │
│    prosessinstansService.opprett...()  ───────┤─ Creates saga, publishes event
│  }                                            │                              │
│                                               ▼                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ BEFORE_COMMIT listeners run here:                                      │ │
│  │  • SaksoppplysningEventListener.lagrePersonopplysninger()              │ │
│  │  • ProsessinstansOpprettetListener.oppdaterProsessinstansstatus()      │ │
│  │  • Other @EventListener methods                                        │ │
│  │                                                                        │ │
│  │ These modify entities IN THE SAME TRANSACTION                          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└───────────────────────────────────────┬──────────────────────────────────────┘
                                        │ COMMIT
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ASYNC SAGA EXECUTION (separate thread)                  │
│                                                                              │
│  AFTER_COMMIT: ProsessinstansOpprettetListener triggers saga                 │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ Each saga step runs in REQUIRES_NEW transaction:                       │ │
│  │  • Step 1: LAGRE_PERSONOPPLYSNINGER                                    │ │
│  │  • Step 2: HENT_REGISTEROPPLYSNINGER                                   │ │
│  │  • Step 3: OPPRETT_FAKTURASERIE                                        │ │
│  │  • ...                                                                 │ │
│  │                                                                        │ │
│  │ These load fresh entities but may conflict with sync listeners         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

**The problem**: Synchronous event listeners and async saga steps can both modify the same
entities, leading to race conditions.

## Anti-Pattern: Synchronous Event Listeners Modifying Entities

### Pattern Description

```java
@Component
public class SomeEventListener {
    @EventListener  // Runs synchronously in triggering transaction
    @Transactional  // Joins the existing transaction
    public void onStatusChange(BehandlingEndretStatusEvent event) {
        // Loads and modifies entities
        behandling.getSaksopplysninger().add(newOpplysning);
    }
}
```

### Why It's Problematic

1. **Runs in HTTP transaction**: Listener executes before saga starts
2. **Saga loads same entities**: Saga steps load fresh copies from DB
3. **Both modify**: If both modify the same entity → conflict

### Real-World Example: SaksopplysningKilde Race Condition

**Error**: `OptimisticLockingException: Row was updated or deleted by another transaction`

```
Time    HTTP Thread (sync)                    Saga Thread (async)
────────────────────────────────────────────────────────────────────────
T1      fattVedtak() starts
T2      Status → IVERKSETTER_VEDTAK
T3      SaksoppplysningEventListener runs
T4        → modifies saksopplysninger
T5      Transaction commits
T6                                            Saga starts
T7                                            HentRegisteropplysninger step
T8                                              → loads saksopplysninger
T9                                              → modifies same collection
T10                                           Hibernate detects version mismatch
T11                                           OptimisticLockingException!
```

**Fix**: Move the event listener logic to a saga step (see LAGRE_PERSONOPPLYSNINGER).

## Anti-Pattern: Loading Entities Across Transaction Boundaries

### Pattern Description

A saga step loads entities for MULTIPLE behandlinger (not just its own):

```kotlin
// In OpprettFakturaserie saga step
behandling.fagsak.behandlinger
    .filter { it.id != behandling.id }  // Other behandlinger on same fagsak
    .forEach {
        val br = behandlingsresultatService.hentBehandlingsresultat(it.id)
        // br is now a managed entity in THIS transaction
    }
```

### Why It's Problematic

1. **Loads OTHER behandling's entities**: Step loads Behandlingsresultat for sibling behandlinger
2. **Creates managed entities**: These become managed in the saga's persistence context
3. **Concurrent modification**: Another thread modifies the same entity
4. **Stale flush**: When saga commits, JPA flushes ALL managed entities, including stale copies

### Real-World Example: Behandlingsresultat.type Overwrite

**Error**: Silent data corruption - no exception, just wrong data

```
Time    Original Saga (Thread A)              Nyvurdering fattVedtak (Thread B)
────────────────────────────────────────────────────────────────────────────────
T1      OpprettFakturaserie step runs
T2      Loads nyvurdering's BR
        (type=IKKE_FASTSATT)
T3                                            fattVedtak() starts
T4                                            Sets type=MEDLEM_I_FOLKETRYGDEN
T5                                            saveAndFlush() - commits
T6      Modifies other fields
T7      Transaction commits
T8      JPA flushes ALL managed entities
T9      → Overwrites type back to IKKE_FASTSATT!
```

**Fix**: Add `@DynamicUpdate` to the entity (see Behandlingsresultat).

## Solutions and Mitigations

### Solution 1: Move Event Listeners to Saga Steps

**When to use**: Listener modifies entities that saga steps also touch.

**Before** (problematic):
```java
@EventListener
public void onStatusChange(BehandlingEndretStatusEvent event) {
    if (event.getStatus() == IVERKSETTER_VEDTAK) {
        saksopplysningerService.lagrePersonopplysninger(behandling);
    }
}
```

**After** (safe):
```kotlin
@Component
class LagrePersonopplysninger : StegBehandler {
    override fun inngangsSteg() = ProsessSteg.LAGRE_PERSONOPPLYSNINGER

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = behandlingService.hent(prosessinstans.hentBehandling.id)
        saksopplysningerService.lagrePersonopplysninger(behandling)
    }
}
```

Add the step to process flow definitions in `ProsessflytDefinisjon.kt`.

### Solution 2: @DynamicUpdate for Concurrent Entity Access

**When to use**: Entity accessed by multiple transactions that modify different fields.

```kotlin
@Entity
@Table(name = "behandlingsresultat")
@DynamicUpdate  // Only UPDATE columns that actually changed
class Behandlingsresultat {
    // ...
}
```

**How it helps**: If Thread A only modifies `lovvalgsperioder` and Thread B only modifies
`type`, their UPDATE statements won't conflict:
- Thread A: `UPDATE ... SET lovvalgsperiode_fom = ?`
- Thread B: `UPDATE ... SET resultat_type = ?`

### Solution 3: Avoid Loading Other Behandlinger's Entities

**When to use**: Saga step only needs to READ data from other behandlinger.

**Before** (loads managed entity):
```kotlin
behandling.fagsak.behandlinger
    .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
    .flatMap { it.trygdeavgiftsperioder }
```

**After** (read-only query, no managed entity):
```kotlin
// Use a read-only query that doesn't create managed entities
trygdeavgiftsperiodeRepository.findAllByFagsakId(fagsak.id)
```

### Solution 4: Detach Entities After Reading

**When to use**: Must load entity but don't want it flushed.

```kotlin
val br = behandlingsresultatService.hentBehandlingsresultat(otherId)
entityManager.detach(br)  // Remove from persistence context
// Now br won't be flushed when transaction commits
val data = br.someField  // Can still read
```

## Decision Framework

When implementing new functionality that involves both sync and async paths:

```
┌─────────────────────────────────────────────────────────────────┐
│ Does the code modify entities that saga steps also modify?       │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
             YES                              NO
              │                               │
              ▼                               ▼
┌─────────────────────────────┐  ┌─────────────────────────────┐
│ Is it currently in an       │  │ Safe to use event listener   │
│ @EventListener?             │  │ or saga step                 │
└─────────────────────────────┘  └─────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────┐
│ MOVE TO SAGA STEP                                                │
│                                                                  │
│ 1. Create new StegBehandler class                               │
│ 2. Add ProsessSteg enum value                                   │
│ 3. Add to relevant ProsessFlyt definitions                      │
│ 4. Add Flyway migration for prosess_steg table                  │
│ 5. Remove logic from event listener                             │
└─────────────────────────────────────────────────────────────────┘
```

For entities with high concurrency:

```
┌─────────────────────────────────────────────────────────────────┐
│ Is the entity modified by multiple independent transactions?    │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
             YES                              NO
              │                               │
              ▼                               ▼
┌─────────────────────────────┐  ┌─────────────────────────────┐
│ Do they modify DIFFERENT    │  │ No special handling needed   │
│ columns?                    │  │                              │
└─────────────────────────────┘  └─────────────────────────────┘
              │
    ┌─────────┴─────────┐
    ▼                   ▼
   YES                  NO
    │                   │
    ▼                   ▼
┌────────────┐  ┌─────────────────────────────┐
│ Add        │  │ Serialize access via        │
│ @Dynamic   │  │ saga step ordering or       │
│ Update     │  │ pessimistic locking         │
└────────────┘  └─────────────────────────────┘
```

## Related Documentation

- **[stale-reference-problem.md](stale-reference-problem.md)** - Deep dive into AFTER_COMMIT stale reference issue
- [Concurrency Control](concurrency.md) - låsReferanse mechanism and entity-level race conditions
- [Architecture](architecture.md) - Transaction boundaries and threading model

### Project Documentation

These docs in `docs/` contain investigation details:
- `docs/prosessinstans-reload-fix.md` - Proposed fix for stale reference problem
- `docs/race-condition-analysis.md` - Analysis of sync/async split issues
- `docs/saksopplysningkilde-equals-hashcode-fix.md` - equals/hashCode issue with @Lob fields
