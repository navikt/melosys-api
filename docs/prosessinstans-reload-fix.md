# Prosessinstans Reload Fix: Root Cause Analysis

## Summary

This document analyzes a proposed fix for the race conditions in the saksflyt saga pattern:
**Reload `Prosessinstans` fresh from the database in the AFTER_COMMIT listener before starting the saga.**

## Current Architecture Problem

### The Event Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST TRANSACTION                             │
│                                                                              │
│  FtrlVedtakService.fattVedtak() {                                           │
│    1. oppdaterBehandlingsresultat()  ←── Modifies Behandlingsresultat       │
│    2. endreStatus(IVERKSETTER_VEDTAK) ←── Modifies Behandling               │
│    3. prosessinstansService.opprettProsessinstans...()                      │
│       └── Creates Prosessinstans with behandling REFERENCE                  │
│       └── publishEvent(ProsessinstansOpprettetEvent(prosessinstans))        │
│           ├── BEFORE_COMMIT: oppdaterStatusOmSkalPåVent() runs NOW          │
│           └── AFTER_COMMIT: queued for after transaction                    │
│    4. dokgenService.produserOgDistribuerBrev()  ←── MORE modifications      │
│    5. oppgaveService.ferdigstillOppgave()       ←── MORE modifications      │
│  }                                                                          │
│                                                                              │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │ COMMIT
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AFTER_COMMIT LISTENER                                   │
│                                                                              │
│  behandleOpprettetProsessinstans(event) {                                   │
│    Prosessinstans prosessinstans = event.hentProsessinstans();              │
│    //                              ↑                                        │
│    //     THIS IS THE PROBLEM: Java object reference from HTTP session      │
│    //     The 'behandling' inside is a DETACHED entity with STALE data      │
│    //                                                                        │
│    prosessinstansBehandler.behandleProsessinstans(prosessinstans);          │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### The Core Issue

The `Prosessinstans` entity (line 44-46 in `Prosessinstans.kt`) holds a **direct JPA reference** to `Behandling`:

```kotlin
@ManyToOne
@JoinColumn(name = "behandling_id")
var behandling: Behandling? = null
```

When the event is published:
1. The `Prosessinstans` Java object contains a reference to `Behandling` loaded in the HTTP session
2. After the HTTP transaction commits, the Hibernate session is **closed**
3. The `Behandling` reference becomes a **detached entity**
4. This detached entity may have **stale data** (data as it existed at step 3, not steps 4-5)

### Why @TransactionalEventListener(phase = AFTER_COMMIT) Isn't Enough

The AFTER_COMMIT phase ensures the **listener runs** after commit, but it doesn't address the **stale object reference** problem:

```java
// Current code in ProsessinstansOpprettetListener.java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
    Prosessinstans prosessinstans = event.hentProsessinstans();  // ← Stale Java object!
    if (!prosessinstans.erPåVent()) {
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
    }
}
```

The `event.hentProsessinstans()` returns the **same Java object** that was created during the HTTP transaction - not a fresh load from the database.

---

## Proposed Fix

### The Solution

Reload `Prosessinstans` fresh from the database in the AFTER_COMMIT listener:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
    UUID prosessinstansId = event.hentProsessinstans().getId();

    // Reload fresh from DB - creates new Hibernate session
    Prosessinstans freshProsessinstans = prosessinstansRepository.findById(prosessinstansId)
        .orElseThrow(() -> new IllegalStateException(
            "Prosessinstans ikke funnet etter commit: " + prosessinstansId));

    if (!freshProsessinstans.erPåVent()) {
        prosessinstansBehandler.behandleProsessinstans(freshProsessinstans);
    }
}
```

### Why This Works

1. **Fresh Hibernate Session**: `findById()` executes in a new persistence context
2. **Fresh Entity Graph**: The `Prosessinstans` and its `behandling` association are loaded from committed DB state
3. **Lazy Loading Works**: When saga steps access `prosessinstans.behandling`, it loads current data
4. **Consistent State**: All data reflects the fully committed transaction

---

## Impact Analysis: What Issues Does This Fix?

### Issue 1: Behandlingsresultat.type Overskrives (MELOSYS-7718) ✅ PARTIALLY FIXED

**Original Problem:**
- HTTP thread sets `type = MEDLEM_I_FOLKETRYGDEN`
- Saga step accesses stale `Behandlingsresultat` through detached `Behandling`
- Upon flush, stale `type` value overwrites the correct one

**With Prosessinstans Reload:**
- Saga loads fresh `Prosessinstans` → fresh `Behandling` → fresh `Behandlingsresultat`
- The `type` field will have the correct committed value

**However:** The `@DynamicUpdate` fix provides additional protection (see below).

### Issue 2: SaksopplysningKilde Konflikt (MELOSYS-7754) ⚠️ NOT DIRECTLY FIXED

**Original Problem:**
- `SaksopplysningEventListener` runs synchronously (BEFORE_COMMIT) modifying `Saksopplysning`
- Saga step `HentRegisteropplysninger` also modifies `Saksopplysning`
- Concurrent modification causes `OptimisticLockingException`

**Status:** Already fixed by PR #3161 (moved listener logic to saga step `LAGRE_PERSONOPPLYSNINGER`)

**The Prosessinstans reload doesn't fix this** - it was an architectural issue requiring the listener-to-saga migration.

### Issue 3: Behandling med Stale OppgaveId (MELOSYS-7803) ✅ FIXED

**Original Problem:**
- `OppgaveService.settOppgaveIdPåBehandling()` used a stale `Behandling` reference
- Saga had updated the same `Behandling` in the meantime
- Version mismatch caused `OptimisticLockingFailureException`

**With Prosessinstans Reload:**
- Fresh `Behandling` reference means current version number
- No version conflicts from stale references

---

## Can We Revert Previous Fixes?

### PR #3160: @DynamicUpdate on Behandlingsresultat ⚠️ RECOMMEND KEEPING

**What it does:**
```kotlin
@Entity
@DynamicUpdate  // Only include changed columns in UPDATE statements
class Behandlingsresultat { ... }
```

**Should we revert?** **NO - Keep it as a safety net.**

**Reasons to keep:**
1. **Defense in Depth**: Even with fresh loads, multiple saga steps might modify the same entity. `@DynamicUpdate` prevents accidental overwrites of fields not touched by the step.
2. **Concurrent External Modifications**: Other processes might modify entities while saga runs.
3. **Minimal Performance Impact**: The only cost is slightly more complex UPDATE SQL.
4. **Best Practice**: It's the correct Hibernate pattern for entities modified from multiple contexts.

### PR #3160: saveAndFlush() Before Saga Start ⚠️ RECOMMEND KEEPING

**What it does:**
```kotlin
behandlingsresultatService.lagreOgFlush(behandlingsresultat)
// Then start saga
```

**Should we revert?** **NO - Keep it.**

**Reasons to keep:**
1. **Explicit Ordering**: Ensures data is written to DB before any read
2. **Debugging**: Makes the write point explicit in logs/profiling
3. **No Downside**: Minor performance impact, significant clarity benefit

### PR #3161: Move SaksopplysningEventListener to Saga Step ❌ CANNOT REVERT

**What it does:** Moved synchronous BEFORE_COMMIT listener logic to saga step `LAGRE_PERSONOPPLYSNINGER`

**Should we revert?** **NO - This is the correct architectural pattern.**

The Prosessinstans reload doesn't solve sync/async conflicts. The listener-to-saga migration is the right fix.

### PR #3166: Reload Behandling in OppgaveService ✅ CAN POTENTIALLY SIMPLIFY

**What it does:**
```kotlin
private fun settOppgaveIdPåBehandling(behandling: Behandling, oppgaveId: String) {
    val freshBehandling = behandlingService.hentBehandling(behandling.id)
    freshBehandling.oppgaveId = oppgaveId
    behandlingService.lagre(freshBehandling)
}
```

**Should we revert?** **MAYBE - but evaluate carefully.**

With Prosessinstans reload, the `behandling` reference passed to `OppgaveService` will be fresh. However:
- If `OppgaveService` is called outside saga context, the reload is still needed
- The current code is explicit and defensive
- **Recommendation**: Keep for now, evaluate as part of larger cleanup

---

## Summary: Fix vs Revert Matrix

| Fix | Type | Can Revert? | Recommendation |
|-----|------|-------------|----------------|
| **Prosessinstans Reload** (new) | Root cause fix | N/A | **IMPLEMENT** |
| PR #3160: `@DynamicUpdate` | Safety net | No | **KEEP** |
| PR #3160: `saveAndFlush()` | Explicit ordering | No | **KEEP** |
| PR #3161: Listener → Saga Step | Architectural fix | No | **KEEP** |
| PR #3166: Reload in OppgaveService | Tactical fix | Maybe | Keep for now |

---

## Remaining Risk After Fix

Even with the Prosessinstans reload fix, some scenarios can still cause issues:

### 1. Long-Running Saga Steps
If a saga step takes a long time and another process modifies the same entities:
- **Mitigation**: `@DynamicUpdate` prevents overwriting unchanged fields
- **Mitigation**: Optimistic locking (`@Version`) detects conflicts

### 2. Saga Steps Using Stale Intermediate State
If saga step A modifies an entity, and step B reads it before A's transaction commits:
- **Current Architecture**: Each step has `REQUIRES_NEW` transaction
- **Mitigation**: Steps should always reload entities they need to modify

### 3. Multiple Sagas on Same Behandling
If two sagas run concurrently on the same Behandling:
- **Mitigation**: `låsReferanse` mechanism prevents this for known conflict cases
- **Mitigation**: Optimistic locking catches remaining conflicts

---

## Implementation Plan

### Step 1: Implement Prosessinstans Reload
Modify `ProsessinstansOpprettetListener.behandleOpprettetProsessinstans()` to reload from DB.

### Step 2: Add Integration Test
Create test that verifies saga sees committed state, not stale reference state.

### Step 3: Monitor in Production
Watch for:
- Reduction in `OptimisticLockingException` errors
- Any new issues from the change

### Step 4: Evaluate Further Cleanup
After stabilization, consider:
- Removing redundant reload calls in individual services
- Standardizing entity reload patterns across saga steps

---

## Conclusion

**The Prosessinstans reload fix addresses the root cause** of the stale reference problem. However, it should be implemented **alongside** the existing tactical fixes, not as a replacement.

The `@DynamicUpdate` annotation and explicit `saveAndFlush()` calls provide valuable defense-in-depth that protects against edge cases the reload fix doesn't cover.

**Recommended approach:**
1. Implement the Prosessinstans reload fix
2. Keep all existing fixes as safety nets
3. Monitor production metrics
4. Evaluate cleanup opportunities after stabilization

---

*Document created: 2025-01-08*
*Related PRs: #3160, #3161, #3166*
*Related JIRA: MELOSYS-7718, MELOSYS-7754, MELOSYS-7803*
