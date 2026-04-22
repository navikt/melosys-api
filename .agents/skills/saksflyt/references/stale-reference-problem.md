# The Stale Reference Problem

## Critical Insight: AFTER_COMMIT Does NOT Prevent Stale Data

A common misconception: "Since the saga starts AFTER_COMMIT, it should see committed data."

**This is WRONG.** Here's why:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST TRANSACTION                             │
│                                                                              │
│  FtrlVedtakService.fattVedtak() {                                           │
│    1. behandlingsresultat.setType(MEDLEM)     ←── Modifies entity           │
│    2. behandling.setStatus(IVERKSETTER)       ←── Modifies entity           │
│    3. prosessinstansService.opprettProsessinstans...()                      │
│       └── Creates Prosessinstans with behandling REFERENCE                  │
│       └── publishEvent(ProsessinstansOpprettetEvent(prosessinstans))        │
│    4. dokgenService.produserBrev()            ←── MORE modifications        │
│    5. oppgaveService.ferdigstill()            ←── MORE modifications        │
│  }                                                                          │
│                                                                              │
│  BEFORE_COMMIT listeners run (same transaction)                             │
└───────────────────────────────────────┬──────────────────────────────────────┘
                                        │ COMMIT (all changes written to DB)
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AFTER_COMMIT LISTENER                                   │
│                                                                              │
│  // ProsessinstansOpprettetListener.java line 26-31                         │
│  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)         │
│  public void behandleOpprettetProsessinstans(event) {                       │
│      Prosessinstans prosessinstans = event.hentProsessinstans();            │
│      //                              ↑                                      │
│      //     THE PROBLEM: Same Java object from HTTP session                 │
│      //     The 'behandling' inside is DETACHED with potentially STALE data │
│      //                                                                      │
│      prosessinstansBehandler.behandleProsessinstans(prosessinstans);        │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### The Distinction

| What AFTER_COMMIT guarantees | What AFTER_COMMIT does NOT guarantee |
|------------------------------|--------------------------------------|
| DB transaction is committed | Java objects have fresh data |
| All changes are persisted | Detached entities are current |
| Saga won't start before commit | Saga will load fresh entities |

### Why This Matters

The `Prosessinstans` entity holds a **direct JPA reference** to `Behandling`:

```kotlin
// Prosessinstans.kt line 44-46
@ManyToOne
@JoinColumn(name = "behandling_id")
var behandling: Behandling? = null
```

When `event.hentProsessinstans()` is called in AFTER_COMMIT:
1. Returns the **same Java object** created during HTTP transaction
2. The `behandling` reference is a **detached entity** (Hibernate session closed)
3. This detached entity has data from **step 3**, not steps 4-5
4. When saga steps access `prosessinstans.behandling`, they get stale data

---

## The Three Flavors of Race Conditions

### Flavor 1: Stale Object Reference (Root Cause)

**Mechanism**: Saga receives detached entity graph from event object

```
HTTP TX:  Create Prosessinstans → event contains object reference
                  ↓
AFTER_COMMIT: event.hentProsessinstans() returns SAME object (detached)
                  ↓
Saga steps: Access prosessinstans.behandling → STALE DATA
```

**Proposed Fix**: Reload Prosessinstans fresh from DB in AFTER_COMMIT:

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

### Flavor 2: Sync/Async Entity Modification Conflict

**Mechanism**: Synchronous event listeners modify same entities as saga steps

```
HTTP TX:  SaksopplysningEventListener modifies Saksopplysning (BEFORE_COMMIT)
              ↓
COMMIT
              ↓
Saga:     HentRegisteropplysninger modifies same Saksopplysning
              ↓
Result:   OptimisticLockingException (version mismatch)
```

**Fix**: Move listener logic to saga step (see PR #3161, LAGRE_PERSONOPPLYSNINGER)

### Flavor 3: Cross-Transaction Entity Loading

**Mechanism**: Saga step loads entities for OTHER behandlinger

```
Saga step:  Load Behandlingsresultat for sibling behandling
                ↓
Other TX:   Modifies same Behandlingsresultat.type
                ↓
Saga flush: JPA flushes ALL managed entities including stale copy
                ↓
Result:     Silent data corruption (type overwritten)
```

**Fix**: @DynamicUpdate on entity (only changed columns in UPDATE)

---

## Investigation Checklist

When debugging saga-related race conditions:

### 1. Identify the Symptom

| Symptom | Likely Flavor |
|---------|---------------|
| `OptimisticLockingException` | Flavor 2 (sync/async conflict) |
| Silent data corruption (field reverts) | Flavor 1 or 3 |
| `StaleObjectStateException` | Flavor 1 (stale reference) |

### 2. Trace the Entity Graph

```sql
-- Find what entities a saga touches
SELECT DISTINCT table_name FROM (
  -- Add your audit/logging query here
);
```

### 3. Check for Sync Listeners

Search for event listeners that trigger on same status as saga:

```bash
grep -r "@EventListener\|@TransactionalEventListener" --include="*.java" --include="*.kt" | \
  grep -v "AFTER_COMMIT"
```

### 4. Check for Cross-Behandling Loading

Search saga steps for loading other behandlinger:

```kotlin
// Anti-pattern to search for:
behandling.fagsak.behandlinger
    .filter { it.id != behandling.id }
    .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
```

### 5. Add Debug Logging

```kotlin
// In VedtakService before saga start
log.info("RACE_DEBUG: Before saga - BR.type=${behandlingsresultat.type}")

// In saga step
log.info("RACE_DEBUG: Saga step sees BR.type=${behandlingsresultat.type}")
```

### 6. Run E2E Tests Repeatedly

Race conditions are timing-dependent. Run failing tests 5-10 times:

```bash
for i in {1..10}; do
  ./run-e2e-test.sh specific-test && echo "Pass $i" || echo "FAIL $i"
done
```

---

## Fix vs Mitigation Matrix

| Fix | Type | What it solves |
|-----|------|----------------|
| **Prosessinstans reload** | Root cause | Flavor 1: Stale object reference |
| **Move listener to saga step** | Architectural | Flavor 2: Sync/async conflict |
| **@DynamicUpdate** | Safety net | Flavor 3: Cross-TX overwrites |
| **saveAndFlush()** | Explicit ordering | Makes write point explicit |
| **Reload in service method** | Tactical | Fresh entity in specific method |

### Recommendation: Defense in Depth

Implement multiple layers:

1. **Prosessinstans reload** - Fixes root cause
2. **Keep @DynamicUpdate** - Catches edge cases
3. **Keep saveAndFlush()** - Explicit ordering
4. **Keep listener migrations** - Correct architecture

---

## Related Issues and PRs

| Issue | Description | Fix |
|-------|-------------|-----|
| MELOSYS-7718 | Behandlingsresultat.type overskrives | @DynamicUpdate + saveAndFlush |
| MELOSYS-7754 | SaksopplysningKilde OptimisticLocking | Listener → saga step |
| MELOSYS-7803 | Behandling stale oppgaveId | Reload in OppgaveService |

---

## SaksopplysningKilde equals/hashCode Issue

A separate but related problem: `SaksopplysningKilde.equals/hashCode` used `@Lob` field:

```java
// PROBLEMATIC - mottattDokument is @Lob (CLOB)
return Objects.equals(this.kilde, that.kilde)
    && Objects.equals(this.mottattDokument, that.mottattDokument);
```

**Why this causes problems:**
1. CLOB representation varies between Hibernate sessions
2. Same content can give different Java String objects
3. Set membership becomes unstable (hashCode changes)
4. `orphanRemoval = true` may trigger unexpected DELETE/INSERT

**Fix**: Use id-based comparison for persisted entities:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SaksopplysningKilde)) return false;
    SaksopplysningKilde that = (SaksopplysningKilde) o;
    if (this.id != null && that.id != null) {
        return this.id.equals(that.id);  // Persisted: compare by id
    }
    // Unpersisted: use business key
    return Objects.equals(this.saksopplysning, that.saksopplysning)
        && Objects.equals(this.kilde, that.kilde);
}

@Override
public int hashCode() {
    return Objects.hash(kilde);  // Only immutable field
}
```

---

## Key Takeaways

1. **AFTER_COMMIT guarantees DB commit, NOT fresh Java objects**
2. **The event object contains the SAME Java reference from HTTP transaction**
3. **Detached entities have stale data from when they were loaded**
4. **Three flavors: stale reference, sync/async conflict, cross-TX loading**
5. **Use defense in depth: reload + @DynamicUpdate + architectural fixes**

---

*Last updated: 2025-01-09*
*Related docs: `docs/prosessinstans-reload-fix.md`, `docs/race-condition-analysis.md`*
