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
service/oppgave/                # Kotlin
├── OppgaveService.kt           # Service facade orchestrating operations
├── OppgaveFactory.kt           # Builds tasks with correct metadata
├── OppgaveGosysMapping.kt      # Complex mapping table for task properties
├── OppgaveBehandlingstema.kt   # ab-code enum (EU_EOS_YRKESAKTIV = "ab0483", …)
├── OppgaveBehandlingstemaUtleder.kt
├── OppgaveBeskrivelseUtleder.kt
├── OppgaveTemaUtleder.kt
├── OppgavetypeUtleder.kt
├── Oppgaveplukker.kt
├── OppgaveSoekFilter.kt
└── migrering/                  # Oppgave-ID backfill jobs

integrasjon/oppgave/            # Java
├── OppgaveFasade.java          # Integration interface (calls Oppgave REST API)
├── OppgaveFasadeImpl.java      # Fasade implementation
├── OppgaveOppdatering.java     # Builder DTO for updates
└── konsument/
    └── OppgaveClient.java      # REST client for the Oppgave API

domain/oppgave/                 # Java
└── Oppgave.java                # Immutable task entity (Builder)
```

There are two layers: `OppgaveService` (service-module facade exposing domain-friendly
operations) delegates to `OppgaveFasade`/`OppgaveFasadeImpl` (integration layer that calls
the external Oppgave REST API via `konsument/OppgaveClient`).

### Key Operations

`OppgaveService` (service layer) and `OppgaveFasade` (integration layer) expose different
method names. Use `OppgaveService` from business code; it delegates to the fasade.

| Operation | OppgaveService method | Description |
|-----------|-----------------------|-------------|
| Create | `opprettOppgave(oppgave)` | Create standard task |
| Fetch | `hentOppgaveMedOppgaveID(oppgaveID)` | Get task by ID |
| Update | `oppdaterOppgave(oppgaveID, oppdatering)` | Update task properties |
| Complete | `ferdigstillOppgave(oppgaveID)` | Mark task as done |
| Return | `leggTilbakeBehandlingsoppgaveMedSaksnummer(saksnummer)` | Unassign task on a case |
| Assign | `tildelOppgave(oppgaveID, saksbehandler)` | Assign to saksbehandler |
| Reuse | `opprettEllerGjenbrukBehandlingsoppgave(...)` | Create or reuse existing |
| Find open | `finnÅpneBehandlingsoppgaverMedFagsaksnummer(saksnummer)` | Open tasks for case |

`OppgaveFasade` exposes the raw integration calls used by the service, including
`opprettSensitivOppgave(oppgave)` (routes to NAV Viken), `hentOppgave(oppgaveId)`,
`leggTilbakeOppgave(oppgaveId)`, `finnOppgaverMedAktørId(aktørID, oppgavetyper)`,
`finnUtildelteOppgaverEtterFrist(behandlingstema)` and
`finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer)`.

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
val oppdatering = OppgaveOppdatering.builder()
    .beskrivelse("Anmodning om unntak er sendt")
    .fristFerdigstillelse(dokumentasjonSvarfrist)
    .build()

oppgaveService.oppdaterOppgave(oppgaveId, oppdatering)
```

### Complete Task
```kotlin
oppgaveService.ferdigstillOppgave(oppgaveId)
```

### Find Tasks
```kotlin
// Open tasks for case (service layer)
val åpne = oppgaveService.finnÅpneBehandlingsoppgaverMedFagsaksnummer(saksnummer)

// Tasks by actor (integration fasade)
val oppgaver = oppgaveFasade.finnOppgaverMedAktørId(aktørId, oppgavetyper)

// Unassigned past deadline (integration fasade)
val forfalt = oppgaveFasade.finnUtildelteOppgaverEtterFrist(behandlingstema)
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

Tasks for protected persons route to NAV Viken (`NAV_VIKEN_ENHET_ID = 2103`) instead of
Melosys / NAV Medlemskap og avgift (`MELOSYS_ENHET_ID = 4530`). Both are defined in
`integrasjon/Konstanter.java`; `OppgaveFasadeImpl` picks the enhet via
`erSensitiv ? NAV_VIKEN_ENHET_ID : MELOSYS_ENHET_ID`.

```kotlin
// Sensitive when:
// - bruker.harStrengtFortroligAdresse() == true
// - OR child has Diskresjonskode.STRENGT_FORTROLIG

if (erSensitiv) {
    oppgaveFasade.opprettSensitivOppgave(oppgave)  // Routes to NAV Viken (2103)
} else {
    oppgaveFasade.opprettOppgave(oppgave)          // Routes to Melosys (4530)
}
```

### Unit IDs

| Unit | ID | Constant | Usage |
|------|-----|----------|-------|
| Melosys (NAV Medlemskap og avgift) | 4530 | `Konstanter.MELOSYS_ENHET_ID` | Standard tasks |
| NAV Viken | 2103 | `Konstanter.NAV_VIKEN_ENHET_ID` | Sensitive/protected persons |

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

A lookup table (`rows: List<TableRow>`) maps the behandling context to a Gosys oppgave.
`finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype)` returns an internal
`Oppgave(oppgaveBehandlingstema, tema, oppgaveType, beskrivelsefelt)` and throws if no row
matches. `finnOppgaveOrNull` falls back through `finnOppgaveFraTabell` →
`finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet` →
`finnOppgaveVedBehandlingstypeHenvendelse`.

| Input (TableRow match) | Output (Oppgave) |
|------------------------|------------------|
| Sakstyper | → oppgaveType (Oppgavetyper) |
| Sakstemaer | → tema (MED / UFM / TRY) |
| Set\<Behandlingstema\> | → oppgaveBehandlingstema (ab-code) |
| Set\<Behandlingstyper\> | → beskrivelsefelt |

**Location**: `service/oppgave/OppgaveGosysMapping.kt`; rows mirror the Confluence
"Oppgaver i Gosys" table and are generated by `OppgaveGosysMappingCodeGenerator`.

## Common Issues

### 1. Multiple Open Tasks

**Symptom**: `TekniskException: "Fant flere åpne oppgaver for sak"`

**Cause**: Data quality issue, duplicate tasks created

**Investigation**: Oppgave data lives in the external Oppgave system, not in a local table
(the only local oppgave-related table is `oppgave_tilbakelegging`). Query via the service:
```kotlin
val åpne = oppgaveService.finnÅpneBehandlingsoppgaverMedFagsaksnummer("MEL-12345")
log.info("Fant ${åpne.size} åpne oppgaver: ${åpne.map { it.oppgaveId }}")
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
-- behandling joins fagsak via the saksnummer FK (no fagsak_saksnummer column)
SELECT b.id, b.oppgave_id, b.status, f.saksnummer
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE b.id = :behandlingId;
```

### Check Task Status in Oppgave System
```kotlin
val oppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveId)
log.info("Status: ${oppgave.status}, tilordnet: ${oppgave.tilordnetRessurs}")
```

### Find All Tasks for Person
```kotlin
// Via Oppgave API search (integration fasade)
oppgaveFasade.finnOppgaverMedAktørId(aktørId, arrayOf("BEH_SAK_MK", "VUR"))
```

## Detailed Documentation

- **[Task Types](references/task-types.md)**: Complete oppgavetype reference
- **[Mapping](references/mapping.md)**: OppgaveGosysMapping details
- **[Debugging](references/debugging.md)**: SQL queries, log patterns, and common-issue recipes
