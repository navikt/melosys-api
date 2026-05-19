---
name: oppgave
description: |
  Expert knowledge of Oppgave (task management) integration in melosys-api.
  Use when: (1) Creating, updating, or closing oppgaver (tasks),
  (2) Understanding task types (BEH_SAK_MK, VUR, JFR) and priorities,
  (3) Debugging task assignment or reuse logic,
  (4) Working with saga steps for oppgave handling,
  (5) Understanding sensitive case routing (NAV Viken vs Melosys unit).
---

# Oppgave Integration

Oppgave is NAV's task management system. Melosys creates tasks for saksbehandlere to work on
when behandlinger are created or need attention.

## Quick Reference

### Module Structure
```
service/oppgave/
├── OppgaveService.kt           # Main service orchestrating operations
├── OppgaveFactory.kt           # Builds tasks with correct metadata
├── OppgaveGosysMapping.kt      # Complex mapping table for task properties
├── OppgaveBeskrivelseUtleder.kt
├── OppgaveTemaUtleder.kt
└── OppgavetypeUtleder.kt

integrasjon/oppgave/
├── OppgaveFasade.java          # Interface for oppgave operations
├── OppgaveFasadeImpl.java      # REST client implementation
├── OppgaveConsumer.kt          # WebClient-based consumer
└── OppgaveOppdatering.kt       # Builder DTO for updates

domain/
└── Oppgave.kt                  # Immutable task entity
```

### Key Operations

| Operation | Method | Description |
|-----------|--------|-------------|
| Create | `opprettOppgave()` | Create standard task |
| Create sensitive | `opprettSensitivOppgave()` | Create task for protected persons |
| Fetch | `hentOppgave()` | Get task by ID |
| Update | `oppdaterOppgave()` | Update task properties |
| Complete | `ferdigstillOppgave()` | Mark task as done |
| Return | `leggTilbakeOppgave()` | Unassign task |
| Assign | `tildelOppgave()` | Assign to saksbehandler |
| Reuse | `opprettEllerGjenbrukBehandlingsoppgave()` | Create or reuse existing |

### Task Types (Oppgavetyper)

| Type | Name | Description |
|------|------|-------------|
| `BEH_SAK_MK` | Behandle sak | Main treatment task |
| `VUR` | Vurder dokument | Document evaluation |
| `JFR` | Journalføring | Document filing |
| `BEH_SED` | Behandle SED | SED document handling |
| `VURD_HENV` | Vurder henvendelse | Evaluate inquiry |
| `VURD_MAN_INNB` | Manglende innbetaling | Missing payment |
| `BEH_ARSAVREG` | Årsavregning | Annual settlement |

### Priorities (PrioritetType)

| Priority | Name |
|----------|------|
| `LAV` | Low |
| `NORM` | Normal (default) |
| `HOY` | High |

### Tema Values

| Tema | Description |
|------|-------------|
| `MED` | Medlemskap (membership) |
| `UFM` | Unntak fra medlemskap |
| `TRY` | Trygdeavgift (social insurance charge) |

## Entity Relationships

```
Fagsak (saksnummer)
├── Behandling
│   ├── oppgaveId ──────────► Oppgave
│   ├── tema                   ├── saksnummer
│   ├── type                   ├── journalpostId
│   └── fristDato              ├── oppgavetype
│                              ├── tema
└── Multiple Oppgaver          ├── behandlingstema
    (over time)                ├── prioritet
                               ├── tilordnetRessurs
                               └── fristFerdigstillelse
```

## Common Operations

### Create or Reuse Task
```kotlin
// Most common pattern - creates new or reuses existing
oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
    behandling = behandling,
    journalpostId = journalpostId,
    aktørId = aktørId,
    tilordnetRessurs = saksbehandlerId,  // Optional
    orgnr = orgnr
)
```

### Update Task
```kotlin
val oppdatering = OppgaveOppdatering.Builder()
    .medBeskrivelse("Anmodning om unntak er sendt")
    .medFrist(dokumentasjonSvarfrist)
    .build()

oppgaveService.oppdaterOppgave(oppgaveId, oppdatering)
```

### Complete Task
```kotlin
oppgaveService.ferdigstillOppgave(oppgaveId)
```

### Find Tasks
```kotlin
// Open tasks for case
val åpne = oppgaveService.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)

// Tasks by actor
val oppgaver = oppgaveService.finnOppgaverMedAktørId(aktørId, oppgavetyper)

// Unassigned past deadline
val forfalt = oppgaveService.finnUtildelteOppgaverEtterFrist(behandlingstema)
```

## Task Reuse Logic

Tasks are reused to avoid duplicates:

```kotlin
// Reuse allowed when:
// 1. Existing open task for same saksnummer
// 2. behandling.type != ÅRSAVREGNING (always new task)

if (harEksisterendeÅpenOppgave && !erÅrsavregning) {
    // Reuse existing task, update if needed
} else {
    // Create new task
}
```

## Sensitive Case Handling

Tasks for protected persons route to NAV Viken (unit 2103) instead of Melosys (unit 4863):

```kotlin
// Sensitive when:
// - bruker.harStrengtFortroligAdresse() == true
// - OR child has Diskresjonskode.STRENGT_FORTROLIG

if (erSensitiv) {
    oppgaveService.opprettSensitivOppgave(oppgave)  // Routes to NAV Viken
} else {
    oppgaveService.opprettOppgave(oppgave)  // Routes to Melosys
}
```

### Unit IDs

| Unit | ID | Usage |
|------|-----|-------|
| Melosys | 4863 | Standard tasks |
| NAV Viken | 2103 | Sensitive/protected persons |

## Saga Steps

| Step | Description |
|------|-------------|
| `OPPRETT_OPPGAVE` | Creates or reuses task |
| `GJENBRUK_OPPGAVE` | Reuses task from prior treatment |
| `OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT` | Updates task when Art. 16 request sent |

### Saga Step Flow
```
OPPRETT_FAGSAK_OG_BEHANDLING
         │
         ▼
   OPPRETT_OPPGAVE  ──► Creates/reuses task
         │                    │
         ▼                    ▼
   (treatment work)     oppgaveId stored
         │              on behandling
         ▼
  IVERKSETT_VEDTAK
         │
         ▼
   ferdigstillOppgave()
```

## OppgaveGosysMapping

Complex mapping determines task properties from behandling context:

| Input | Output |
|-------|--------|
| Sakstype | → Oppgavetype |
| Sakstema | → Tema |
| Behandlingstema | → Behandlingstema code |
| Behandlingstype | → Priority |

**Location**: `OppgaveGosysMapping.kt` with mapping table from Confluence.

## Common Issues

### 1. Multiple Open Tasks

**Symptom**: `TekniskException: "Fant flere åpne oppgaver for sak"`

**Cause**: Data quality issue, duplicate tasks created

**Investigation**:
```sql
SELECT o.oppgave_id, o.status, o.oppgavetype
FROM oppgave o
WHERE o.saksnummer = 'MEL-12345'
AND o.status IN ('OPPRETTET', 'AAPNET', 'UNDER_BEHANDLING');
```

### 2. Task Not Found

**Symptom**: `IkkeFunnetException` when fetching task

**Cause**: Wrong oppgaveId or task deleted

**Investigation**:
```kotlin
// Check behandling has oppgaveId
val oppgaveId = behandling.oppgaveId
// Verify in Oppgave API
```

### 3. Sensitive Case Not Routed

**Symptom**: Protected person's task visible to wrong unit

**Investigation**:
```kotlin
// Check PDL data
val harFortrolig = pdlService.harStrengtFortroligAdresse(aktørId)
// Check children protection
val barnMedBeskyttelse = // query dependent children
```

### 4. Missing Deadline

**Symptom**: `FunksjonellException: "Mangler fristFerdigstillelse"`

**Cause**: Behandling doesn't have deadline set

**Resolution**: Set `behandling.behandlingsfrist` before creating task

## Debugging

### Find Task for Behandling
```sql
SELECT b.id, b.oppgave_id, b.status, f.saksnummer
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.fagsak_saksnummer
WHERE b.id = :behandlingId;
```

### Check Task Status in Oppgave System
```kotlin
val oppgave = oppgaveService.hentOppgave(oppgaveId)
log.info("Status: ${oppgave.status}, tilordnet: ${oppgave.tilordnetRessurs}")
```

### Find All Tasks for Person
```sql
-- Via Oppgave API search
oppgaveService.finnOppgaverMedAktørId(aktørId, arrayOf("BEH_SAK_MK", "VUR"))
```

## Detailed Documentation

- **[Task Types](references/task-types.md)**: Complete oppgavetype reference
- **[Mapping](references/mapping.md)**: OppgaveGosysMapping details
