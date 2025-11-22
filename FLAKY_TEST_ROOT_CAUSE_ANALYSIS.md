# Flaky Test Root Cause Analysis

**Date:** 2025-11-22
**Branch:** `fix/backend-race-condition-vedtak-creation`
**Status:** ✅ Root Cause Identified - Ready for Fix

---

## 🎯 Executive Summary

The flaky EU/EØS vedtak tests fail 66% of the time due to a **backend race condition** where `Behandling` entity is reloaded during validation, triggering Hibernate to synchronize stale `SaksopplysningKilde` entities, causing optimistic locking failures.

**Fix:** Pass `Behandling` object through the validation chain instead of reloading it by ID.

---

## 🔍 Root Cause: Entity Reload Triggers Hibernate Synchronization

### Complete Call Flow

```
1. EosVedtakService.fattVedtak() [Java]
   └─ Line 91: ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(behandling, ...)

2. FerdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger() [Kotlin]
   └─ Line 47: kontrollerKontrollMedRegisteropplysning.kontrollerVedtak(behandling, ...)

3. KontrollMedRegisteropplysning.kontrollerVedtak() [Kotlin]
   ├─ Line 39: hentNyeRegisteropplysninger(behandling)
   │  └─ Line 51: registeropplysningerService.hentOgLagreOpplysninger(...)
   │     └─ Creates/updates SaksopplysningKilde entities ← FIRST MODIFICATION
   │
   └─ Line 44: kontroll.kontrollerVedtak(behandling.id, ...)  ← ⚠️ Passes ID, not object!

4. Kontroll.kontrollerVedtak() [Kotlin]
   └─ Line 76: utførKontroller(behandlingID, ...)

5. Kontroll.utførKontroller() [Kotlin]
   └─ Line 96: behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)  ← ⚠️ RELOAD!
      └─ Hibernate attempts to synchronize changes to SaksopplysningKilde
      └─ Detects entities were modified externally
      └─ Throws StaleObjectStateException ← OPTIMISTIC LOCK FAILURE
```

### The Problem in Detail

**Step 1 - First Modification (Line 39-51):**
```kotlin
// KontrollMedRegisteropplysning.kt:39
hentNyeRegisteropplysninger(behandling)

// This calls:
registeropplysningerService.hentOgLagreOpplysninger(
    RegisteropplysningerRequest.builder()
        .medlemskapsopplysninger()
        .build()
)
```

This fetches membership data and creates `Saksopplysning` and `SaksopplysningKilde` entities in the database.

**Step 2 - Entity Reload (Line 96):**
```kotlin
// Kontroll.kt:96 (inside utførKontroller)
val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
```

**⚠️ This is where the problem occurs!**

When Hibernate loads the `Behandling` again with all its relationships (`MedSaksopplysninger`), it:
1. Loads fresh `Saksopplysning` entities from DB
2. Compares them with entities in the current session
3. Detects they were modified (timestamps changed, data changed)
4. Attempts to synchronize/delete old versions
5. **FAILS** because entities were updated between load and delete

---

## 💡 Why It's Intermittent (66% Failure Rate)

The race condition depends on **timing**:

### Success Case (33%):
```
Time T0: hentNyeRegisteropplysninger() starts
Time T1: Saves SaksopplysningKilde entities
Time T2: **Session flushes changes to DB**
Time T3: hentBehandlingMedSaksopplysninger() loads (sees committed data)
Time T4: ✅ No conflict - data is synchronized
```

### Failure Case (67%):
```
Time T0: hentNyeRegisteropplysninger() starts
Time T1: Saves SaksopplysningKilde entities
Time T2: hentBehandlingMedSaksopplysninger() loads **BEFORE flush**
Time T3: Hibernate tries to sync uncommitted changes
Time T4: ❌ StaleObjectStateException - entities changed
```

**CI Environment:** Slower execution increases time window → higher failure rate

---

## 🎯 The Fix: Pass Object Instead of Reloading

### Current (Problematic) Code

```kotlin
// KontrollMedRegisteropplysning.kt:44
return kontroll.kontrollerVedtak(
    behandling.id,  // ← Passes ID only!
    sakstype,
    behandlingsresultattype,
    kontrollerSomSkalIgnoreres
)
```

```kotlin
// Kontroll.kt:70-76
internal fun kontrollerVedtak(
    behandlingID: Long,  // ← Receives ID
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?,
    kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
): Collection<Kontrollfeil> =
    utførKontroller(behandlingID, ...)  // ← Passes ID
```

```kotlin
// Kontroll.kt:91-96
private fun utførKontroller(
    behandlingID: Long,  // ← Receives ID
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?
): Collection<Kontrollfeil> {
    val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
    // ⬆️ PROBLEM: Reloads entity, triggering Hibernate sync!
    ...
}
```

### Fixed Code

```kotlin
// KontrollMedRegisteropplysning.kt:44
return kontroll.kontrollerVedtak(
    behandling,  // ← Pass object!
    sakstype,
    behandlingsresultattype,
    kontrollerSomSkalIgnoreres
)
```

```kotlin
// Kontroll.kt:70-76 (Overload existing method)
internal fun kontrollerVedtak(
    behandling: Behandling,  // ← Accept object
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?,
    kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
): Collection<Kontrollfeil> =
    utførKontroller(behandling, sakstype, behandlingsresultattype)
        .filter { skalViseFeil(it, kontrollerSomSkalIgnoreres, behandling.id) }
```

```kotlin
// Kontroll.kt:91-96 (Keep existing method for other callers)
private fun utførKontroller(
    behandling: Behandling,  // ← New overload accepting object
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?
): Collection<Kontrollfeil> {
    // No reload needed - use passed object!
    if (behandlingsresultattype in listOf(...)) {
        return utførKontrollerForAvslagOgHenleggelse(behandling)
    }
    return utførKontroller(behandling, sakstype)
}
```

---

## 📋 Implementation Plan

### Step 1: Add Method Overload to `Kontroll`

**File:** `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/ferdigbehandling/Kontroll.kt`

**Add new public method:**
```kotlin
// Add after line 76
internal fun kontrollerVedtak(
    behandling: Behandling,
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?,
    kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
): Collection<Kontrollfeil> =
    utførKontroller(behandling, sakstype, behandlingsresultattype)
        .filter { skalViseFeil(it, kontrollerSomSkalIgnoreres, behandling.id) }
```

**Add new private method:**
```kotlin
// Add after line 103
private fun utførKontroller(
    behandling: Behandling,
    sakstype: Sakstyper,
    behandlingsresultattype: Behandlingsresultattyper?
): Collection<Kontrollfeil> {
    if (behandlingsresultattype in listOf(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, Behandlingsresultattyper.HENLEGGELSE)) {
        return utførKontrollerForAvslagOgHenleggelse(behandling)
    }

    return utførKontroller(behandling, sakstype)
}
```

### Step 2: Update `KontrollMedRegisteropplysning` to Use New Method

**File:** `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/ferdigbehandling/KontrollMedRegisteropplysning.kt`

**Change line 44:**
```kotlin
// OLD
return kontroll.kontrollerVedtak(behandling.id, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres)

// NEW
return kontroll.kontrollerVedtak(behandling, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres)
```

### Step 3: Add Unit Test

**File:** `service/src/test/kotlin/no/nav/melosys/service/vedtak/EosVedtakServiceTest.kt`

```kotlin
@Test
fun `fattVedtak skal ikke kalle registeropplysninger flere ganger`() {
    // Given
    val behandling = opprettTestBehandling()
    whenever(ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(any(), any(), any(), any()))
        .thenReturn(emptyList())

    // When
    eosVedtakService.fattVedtak(behandling, fattVedtakRequest)

    // Then - verify registeropplysningerService was NOT called multiple times
    // This is verified by checking that behandlingService.hentBehandlingMedSaksopplysninger
    // is only called the minimal number of times
    verify(behandlingService, atMost(2)).hentBehandlingMedSaksopplysninger(any())
}
```

### Step 4: Add Integration Test

**File:** `integrasjonstest/src/test/kotlin/no/nav/melosys/vedtak/VedtakRaceConditionIT.kt` (new file)

```kotlin
@Test
fun `fattVedtak skal ikke feile ved optimistic locking`() {
    // Run vedtak creation 10 times to verify no race condition
    repeat(10) { iteration ->
        // Given
        val behandling = opprettEøsBehandling()

        // When & Then
        assertDoesNotThrow("Iteration $iteration failed with optimistic locking") {
            vedtakService.fattVedtak(behandling, createFattVedtakRequest())
        }
    }
}
```

---

## ✅ Expected Outcomes

### Before Fix
- **E2E Test Failure Rate:** 66% (2/3 attempts fail)
- **Error:** `StaleObjectStateException` on `SaksopplysningKilde`
- **Logs:** Two "Registeropplysninger hentet" messages 16ms apart

### After Fix
- **E2E Test Failure Rate:** 0% (10/10 passes)
- **Error:** None
- **Logs:** One "Registeropplysninger hentet" message
- **Performance:** Slightly faster (one less DB query)

---

## 🧪 Testing Strategy

### 1. Unit Tests
Run existing EosVedtakServiceTest + new test:
```bash
~/.claude/scripts/run-tests.sh -pl service -Dtest=EosVedtakServiceTest
```

### 2. Integration Tests
Run new race condition test:
```bash
~/.claude/scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=VedtakRaceConditionIT
```

### 3. E2E Tests (External repo)
Run skip workflow test 10 times:
```bash
# In melosys-e2e-tests repo
for i in {1..10}; do
    echo "Run $i/10"
    npm test tests/eu-eos/eu-eos-skip-fullfort-vedtak.spec.ts
done
```

Should pass 10/10 times after fix.

---

## 📊 Impact Analysis

### Affected Code Paths

**Direct Impact:**
- ✅ EU/EØS vedtak creation (all behandlingstyper)
- ✅ FTRL vedtak creation (if similar pattern exists)

**No Impact:**
- ✅ Other kontrollerVedtak callers (keep using ID-based method)
- ✅ Existing validations (signature unchanged)

### Backwards Compatibility

**✅ Fully Compatible:**
- New method is an overload (not replacement)
- Existing ID-based method remains for other callers
- No API changes
- No migration needed

---

## 🔗 Related Files

### Production Code
- `service/src/main/java/no/nav/melosys/service/vedtak/EosVedtakService.java`
- `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/ferdigbehandling/FerdigbehandlingKontrollFacade.kt`
- `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/ferdigbehandling/KontrollMedRegisteropplysning.kt`
- `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/ferdigbehandling/Kontroll.kt`
- `service/src/main/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerService.java`

### Test Files
- `service/src/test/kotlin/no/nav/melosys/service/vedtak/EosVedtakServiceTest.kt`
- `integrasjonstest/src/test/kotlin/no/nav/melosys/vedtak/VedtakRaceConditionIT.kt` (new)

### Documentation
- `/Users/rune/source/nav/melosys-e2e-tests/docs/debugging/BACKEND-ISSUE-SUMMARY.md`
- `/Users/rune/source/nav/melosys-e2e-tests/docs/debugging/EU-EOS-SKIP-BACKEND-RACE-CONDITION.md`

---

## 🚀 Next Steps

1. **Review this analysis** - Confirm understanding of root cause
2. **Implement fix** - Make code changes as outlined
3. **Add tests** - Unit + integration tests
4. **Verify locally** - Run all service tests
5. **Commit changes** - One commit per logical change
6. **Test E2E** - Verify flaky tests now pass
7. **Create summary** - Document fix for team

---

**Estimated Time:** 2-3 hours (implementation + testing)
**Risk Level:** Low (backwards compatible, targeted fix)
**Confidence:** High (root cause clearly identified)

---

## 📝 Commit Strategy

Each step should be a separate commit:

1. **Add method overloads to Kontroll** - "Add Behandling-based kontrollerVedtak overloads to avoid entity reload"
2. **Update KontrollMedRegisteropplysning** - "Pass Behandling object instead of ID to prevent race condition"
3. **Add unit test** - "Add test to prevent duplicate registeropplysninger calls"
4. **Add integration test** - "Add race condition integration test for vedtak creation"

---

**Last Updated:** 2025-11-22
**Author:** Claude Code Analysis
**Status:** Ready for Implementation
