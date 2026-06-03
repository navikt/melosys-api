# Oppgave Debugging Guide

## Common SQL Queries

### Find Task for Behandling
```sql
-- behandling joins fagsak via the saksnummer FK (there is no fagsak_saksnummer column)
SELECT b.id as behandling_id, b.oppgave_id, b.status, f.saksnummer
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE b.id = :behandlingId;
```

### Find All Tasks for Case
```kotlin
// Note: Oppgave data is in the external system, not local DB. Use OppgaveService.
oppgaveService.finnÅpneBehandlingsoppgaverMedFagsaksnummer("MEL-12345")
oppgaveService.finnSisteAvsluttetBehandlingsoppgaveMedFagsaksnummer("MEL-12345")
```

### Find Behandlinger Without Tasks
```sql
SELECT b.id, b.status, f.saksnummer, b.oppgave_id
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE b.oppgave_id IS NULL
AND b.status NOT IN ('AVSLUTTET')
ORDER BY b.registrert_dato DESC;
```

### Find Saga Steps Related to Oppgave
```sql
-- prosessinstans columns: prosess_type, steg (FK to prosess_steg); no status/sist_utforte_steg
SELECT pi.uuid, pi.prosess_type, pi.steg, pi.antall_retry
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
ORDER BY pi.registrert_dato DESC;
```

## Common Issues

### Issue: Multiple Open Tasks

**Symptom**:
```
TekniskException: "Fant flere åpne oppgaver for sak MEL-12345"
```

**Cause**: Data quality issue - duplicate tasks created

**Investigation**:
```kotlin
val oppgaver = oppgaveService.finnÅpneBehandlingsoppgaverMedFagsaksnummer(saksnummer)
log.info("Found ${oppgaver.size} open tasks: ${oppgaver.map { it.oppgaveId }}")
```

**Resolution**:
- Ferdigstill duplicate tasks manually
- Investigate why duplicates were created

### Issue: Task Not Found

**Symptom**:
```
IkkeFunnetException: "Fant ikke oppgave med id 12345"
```

**Cause**:
- Wrong oppgaveId stored on behandling
- Task deleted in Oppgave system
- Integration connectivity issue

**Investigation**:
```sql
-- Check stored oppgaveId
SELECT oppgave_id FROM behandling WHERE id = :behandlingId;
```

```kotlin
// Verify task exists
try {
    oppgaveService.hentOppgaveMedOppgaveID(oppgaveId)
} catch (e: IkkeFunnetException) {
    log.error("Task not found in Oppgave system")
}
```

### Issue: Task Created for Wrong Unit

**Symptom**: Protected person's task visible to standard unit

**Cause**: Sensitive case detection failed

**Investigation**:
```kotlin
// Check PDL fortrolig status
val harFortrolig = pdlService.harStrengtFortroligAdresse(aktørId)
log.info("Har strengt fortrolig adresse: $harFortrolig")

// Check children protection
val barn = pdlService.hentBarn(aktørId)
val barnMedBeskyttelse = barn.any { it.harDiskresjonskode() }
log.info("Barn med beskyttelse: $barnMedBeskyttelse")
```

**Expected routing**:
- Sensitive → NAV Viken (2103, `Konstanter.NAV_VIKEN_ENHET_ID`)
- Standard → Melosys / NAV Medlemskap og avgift (4530, `Konstanter.MELOSYS_ENHET_ID`)

### Issue: Task Reuse Not Working

**Symptom**: New task created when should reuse existing

**Investigation**:
```kotlin
// Check existing open tasks
val eksisterende = oppgaveService.finnÅpneBehandlingsoppgaverMedFagsaksnummer(saksnummer)
log.info("Existing open tasks: ${eksisterende.size}")

// Check if ÅRSAVREGNING (always new task)
log.info("Behandling type: ${behandling.type}")
```

**Reuse conditions**:
1. Open task exists for same saksnummer
2. behandling.type != ÅRSAVREGNING

### Issue: Missing Deadline

**Symptom**:
```
FunksjonellException: "Mangler fristFerdigstillelse"
```

**Cause**: Behandling doesn't have deadline set

**Investigation**:
```sql
SELECT id, behandlingsfrist FROM behandling WHERE id = :behandlingId;
```

**Resolution**: Set deadline before creating task

### Issue: Wrong Task Type

**Symptom**: Task appears in wrong Gosys queue

**Investigation**:
```kotlin
// Check mapping (returns an internal OppgaveGosysMapping.Oppgave)
val oppgave = OppgaveGosysMapping().finnOppgave(
    fagsak.sakstype, fagsak.sakstema, behandling.behandlingstema, behandling.behandlingstype
)
log.info("Mapping result: oppgaveType=${oppgave.oppgaveType}, " +
    "tema=${oppgave.tema}, behandlingstema=${oppgave.oppgaveBehandlingstema?.kode}")
```

## Log Patterns

### Oppgave Service
```bash
# All oppgave operations
grep "OppgaveService" application.log

# Task creation
grep "opprettOppgave\|opprettEllerGjenbruk" application.log

# Task updates
grep "oppdaterOppgave\|ferdigstillOppgave" application.log
```

### Oppgave Client (REST calls)
```bash
# REST calls to Oppgave API
grep "OppgaveClient" application.log

# Errors
grep "OppgaveClient" application.log | grep -i "error\|exception"
```

### Saga Steps
```bash
grep "OpprettOppgave\|GjenbrukOppgave" application.log
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Service facade | `service/.../oppgave/OppgaveService.kt` |
| Task Factory | `service/.../oppgave/OppgaveFactory.kt` |
| Gosys Mapping | `service/.../oppgave/OppgaveGosysMapping.kt` |
| REST Client | `integrasjon/.../oppgave/konsument/OppgaveClient.java` |
| Integration Fasade | `integrasjon/.../oppgave/OppgaveFasadeImpl.java` |
| Domain | `domain/.../oppgave/Oppgave.java` |
| Saga Steps | `saksflyt/.../steg/oppgave/` (OpprettOppgave, GjenbrukOppgave, …) |

## Manual Operations

### Fetch Task
```kotlin
val oppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveId)
```

### Complete Task Manually
```kotlin
oppgaveService.ferdigstillOppgave(oppgaveId)
```

### Reassign Task
```kotlin
oppgaveService.tildelOppgave(oppgaveId, nySaksbehandlerId)
```

### Return Task to Pool
```kotlin
// Service layer works on a case; the integration fasade unassigns by oppgaveId
oppgaveService.leggTilbakeBehandlingsoppgaveMedSaksnummer(saksnummer)
oppgaveFasade.leggTilbakeOppgave(oppgaveId)
```

## Oppgave API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/oppgaver/{id}` | GET | Fetch single task |
| `/oppgaver` | GET | Search with params |
| `/oppgaver` | POST | Create task |
| `/oppgaver/{id}` | PUT | Update task |

