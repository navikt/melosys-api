# Oppgave Debugging Guide

## Common SQL Queries

### Find Task for Behandling
```sql
SELECT b.id as behandling_id, b.oppgave_id, b.status, f.saksnummer
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.fagsak_saksnummer
WHERE b.id = :behandlingId;
```

### Find All Tasks for Case
```sql
-- Note: Oppgave data is in external system, not local DB
-- Use OppgaveService to query
oppgaveService.finnÅpneBehandlingsoppgaverMedSaksnummer("MEL-12345")
oppgaveService.finnAvsluttetBehandlingsoppgaverMedSaksnummer("MEL-12345")
```

### Find Behandlinger Without Tasks
```sql
SELECT b.id, b.status, f.saksnummer, b.oppgave_id
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.fagsak_saksnummer
WHERE b.oppgave_id IS NULL
AND b.status NOT IN ('AVSLUTTET', 'LUKKET')
ORDER BY b.registrert_dato DESC;
```

### Find Saga Steps Related to Oppgave
```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.sist_utforte_steg IN ('OPPRETT_OPPGAVE', 'GJENBRUK_OPPGAVE',
    'OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT')
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
val oppgaver = oppgaveService.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)
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
    oppgaveService.hentOppgave(oppgaveId)
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
- Sensitive → NAV Viken (2103)
- Standard → Melosys (4863)

### Issue: Task Reuse Not Working

**Symptom**: New task created when should reuse existing

**Investigation**:
```kotlin
// Check existing open tasks
val eksisterende = oppgaveService.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)
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
// Check mapping
val mapping = OppgaveGosysMapping.finnMapping(
    fagsak.type, fagsak.tema, behandling.tema, behandling.type
)
log.info("Mapping result: oppgavetype=${mapping.oppgavetype}, " +
    "tema=${mapping.tema}, behandlingstema=${mapping.behandlingstemaCode}")
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

### Oppgave Consumer (REST calls)
```bash
# REST calls to Oppgave API
grep "OppgaveConsumer" application.log

# Errors
grep "OppgaveConsumer" application.log | grep -i "error\|exception"
```

### Saga Steps
```bash
grep "OpprettOppgave\|GjenbrukOppgave" application.log
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `service/.../oppgave/OppgaveService.kt` |
| Task Factory | `service/.../oppgave/OppgaveFactory.kt` |
| Gosys Mapping | `service/.../oppgave/OppgaveGosysMapping.kt` |
| REST Consumer | `integrasjon/.../oppgave/OppgaveConsumer.kt` |
| Facade | `integrasjon/.../oppgave/OppgaveFasadeImpl.java` |
| Domain | `domain/.../Oppgave.kt` |
| Saga Steps | `saksflyt/.../steg/oppgave/` |

## Manual Operations

### Fetch Task
```kotlin
val oppgave = oppgaveService.hentOppgave(oppgaveId)
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
oppgaveService.leggTilbakeOppgave(oppgaveId)
```

## Oppgave API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/oppgaver/{id}` | GET | Fetch single task |
| `/oppgaver` | GET | Search with params |
| `/oppgaver` | POST | Create task |
| `/oppgaver/{id}` | PUT | Update task |

