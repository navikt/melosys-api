# Concurrency Control

## Table of Contents
1. [Overview](#overview)
2. [LåsReferanse Mechanism](#låsreferanse-mechanism)
3. [Waiting and Release](#waiting-and-release)
4. [Race Condition Scenarios](#race-condition-scenarios)
5. [Known Vulnerabilities](#known-vulnerabilities)
6. [Debugging Concurrency Issues](#debugging-concurrency-issues)

## Overview

The saksflyt module handles concurrency through a "låsReferanse" (lock reference) mechanism.
This ensures that related sagas are processed sequentially to avoid data corruption.

**Key principle**: Sagas with the same "group prefix" in their låsReferanse wait for each
other to complete before starting.

## LåsReferanse Mechanism

### Lock Reference Types

**Location**: `saksflyt-api/src/main/kotlin/.../domain/LåsReferanseType.kt`

| Type | Pattern | Example | Use Case |
|------|---------|---------|----------|
| `SED` | `^\d+_[a-zA-Z0-9]+_\d+$` | `12345_A001_1` | RINA case SEDs |
| `UBETALT` | `^UBETALT_.\w+_\d+$` | `UBETALT_REF123_456` | Missing payments |

### Lock Reference Structure

#### SedLåsReferanse
**Location**: `saksflyt-api/src/main/kotlin/.../domain/SedLåsReferanse.kt`

Format: `{rinaSaksnummer}_{sedID}_{sedVersjon}`

```kotlin
class SedLåsReferanse(val låsReferanse: String) : LåsReferanse {
    val rinaSaksnummer: String  // Group prefix - all SEDs in same RINA case share this
    val sedID: String
    val sedVersjon: String

    override val gruppePrefiks: String
        get() = rinaSaksnummer  // SEDs in same case wait for each other

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        return aktiveLåsReferanser.isNotEmpty()  // Wait if ANY active lock in same group
    }
}
```

**Example**: For RINA case 12345:
- SED A001 v1: `12345_A001_1`
- SED A002 v1: `12345_A002_1`
- Both have gruppePrefiks `12345`, so they wait for each other

#### ManglendeInnbetalingBehandlingLåsReferanse
Format: `UBETALT_{fakturaserieReferanse}_{fakturanummer}`

Used for missing payment handling - ensures sequential processing per invoice.

### How Locking Works

**ProsessinstansBehandlerDelegate** checks if saga should wait:

```kotlin
private fun skalSettesPåVent(prosessinstans: Prosessinstans): Boolean {
    if (prosessinstans.låsReferanse == null) return false  // No lock = no waiting

    val låsReferanse = LåsReferanseFactory.lagLåsReferanse(prosessinstans.hentLåsReferanse)
    val andreAktive = finnAndreAktiveLåsMedSammegruppePrefiks(prosessinstans.id!!, låsReferanse.gruppePrefiks)
    return låsReferanse.skalSettesPåVent(andreAktive)
}
```

The query finds active sagas with same group prefix:
```java
@Query("""
    SELECT ... FROM Prosessinstans p
    WHERE p.id <> ?1
    AND p.status NOT IN (?2)  -- Excludes PÅ_VENT and FERDIG
    AND p.låsReferanse LIKE CONCAT(?3, '%')
""")
Collection<ProsessinstansInfo> findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
    UUID id, Collection<ProsessStatus> prosessStatus, String låsReferanse);
```

## Waiting and Release

### Setting to PÅ_VENT

The wait check happens in **BEFORE_COMMIT** phase of transaction:

```kotlin
// ProsessinstansOpprettetListener
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
fun oppdaterProsessinstansstatus(event: ProsessinstansOpprettetEvent) {
    prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(event.hentProsessinstans())
}
```

### Releasing Waiting Sagas

When a saga completes, `ProsessinstansFerdigListener` releases the next:

```kotlin
@EventListener
fun prosessinstansFerdig(event: ProsessinstansFerdigEvent) {
    if (event.låsReferanse == null) return

    if (kanNesteProsessinstansStartes(event)) {
        startNesteProsessinstans(event)
    }
}
```

**Priority order for next saga**:
1. **Sub-processes**: Sagas with `parentId == current saga's ID`
2. **Siblings**: Sagas with same `parentId` AND same `låsReferanse`
3. **Oldest in group**: Fallback to oldest saga with same group prefix

## Race Condition Scenarios

### Scenario 1: Concurrent SED Reception

```
Time    Thread A (SED A001)         Thread B (SED A002)
────────────────────────────────────────────────────────────
T1      Create prosessinstans
T2      Check locks: none found     Create prosessinstans
T3      Set KLAR                    Check locks: none found  ← RACE!
T4      Commit TX                   Set KLAR
T5      Start execution             Commit TX
T6      Processing...               Start execution  ← Both running!
```

**Root cause**: The lock check happens in the same transaction as creation.
Two concurrent transactions can both pass the check before either commits.

### Scenario 2: Event Ordering

```
Time    Saga A                      Saga B (same lock group)
────────────────────────────────────────────────────────────
T1      Start step 5
T2                                  Created, set PÅ_VENT
T3      Step 5 completes
T4      Publish FERDIG event
T5      Listener runs: finds B
T6      Starts B
T7      But wait - step 5's TX      B sees stale data!
        hasn't committed yet
```

**Root cause**: Event published before step transaction commits.

### Scenario 3: Status Update Race

```
Time    Behandler                   FerdigListener
────────────────────────────────────────────────────────────
T1      Save: status=FERDIG
T2                                  Query: status=PÅ_VENT
T3                                  Found waiting saga
T4      Commit TX
T5                                  Save: status=KLAR
T6                                  Start saga
T7      Both now have same group active!
```

## Known Vulnerabilities

### 1. No Database-Level Locking
The current implementation uses application-level checks, not database locks.
Concurrent transactions can pass checks before either commits.

**Mitigation**: Use `SELECT ... FOR UPDATE` or database advisory locks.

### 2. BEFORE_COMMIT Check Timing
The låsReferanse check runs before the creating transaction commits.
Other transactions can't see the new saga yet.

**Mitigation**: Consider pessimistic locking or retry logic.

### 3. Event-Transaction Mismatch
`ProsessinstansFerdigEvent` published before step transaction commits.
Listener may start next saga with stale data view.

**Mitigation**: Ensure events published AFTER_COMMIT.

### 4. Thread Pool Saturation
Only 3 threads in pool. If all 3 are blocked/slow, queue builds up.
New sagas may timeout waiting.

**Mitigation**: Monitor queue size, adjust pool or add timeout handling.

### 5. 24-Hour Recovery Window
Stuck sagas only recovered after 24 hours on restart.
Long-running issues may not be noticed.

**Mitigation**: Add alerting, reduce recovery window.

## Debugging Concurrency Issues

### Queries for Investigation

Find active sagas for a RINA case:
```sql
SELECT id, type, status, lås_referanse, endret_dato
FROM prosessinstans
WHERE lås_referanse LIKE '12345%'  -- RINA case number
AND status NOT IN ('FERDIG', 'FEILET')
ORDER BY registrert_dato;
```

Find stuck sagas:
```sql
SELECT id, type, status, lås_referanse, registrert_dato, endret_dato
FROM prosessinstans
WHERE status = 'UNDER_BEHANDLING'
AND endret_dato < SYSDATE - INTERVAL '1' HOUR;
```

Check for overlapping active sagas:
```sql
SELECT p1.id as saga1, p2.id as saga2,
       p1.lås_referanse, p1.status as status1, p2.status as status2
FROM prosessinstans p1
JOIN prosessinstans p2 ON p1.id < p2.id
WHERE p1.lås_referanse LIKE SUBSTR(p2.lås_referanse, 1, INSTR(p2.lås_referanse, '_') - 1) || '%'
AND p1.status = 'UNDER_BEHANDLING'
AND p2.status = 'UNDER_BEHANDLING';
```

### Log Patterns

Look for these log patterns:

```
# Saga starting
"Starter behandling av prosessinstans {} med lås {}"

# Waiting due to lock
"Prosessinstans {} med låsreferanse {} satt på vent"

# Lock group info
"Låsreferanse: {} Andre aktive med samme gruppe prefiks: {}"

# Saga completing
"Prosessinstans {} behandlet ferdig"

# Releasing next
"Prosessinstans {} med låsreferanse {} startes opp etter å ha vært på vent"
```

### Metrics

Check these Micrometer metrics:
- `prosessinstanser.opprettet` - Saga creation rate by type
- `prosessinstanser.steg.utført` - Step completion by type and status
- `prosessinstanser.tid.brukt` - Saga duration by type
- Queue size in `saksflytThreadPoolTaskExecutor`

### Admin Endpoints

The `ProsessinstansAdminController` provides:
- List active/failed sagas
- Restart failed sagas
- Health check for stuck sagas
