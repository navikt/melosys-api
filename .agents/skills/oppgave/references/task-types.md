# Oppgave Task Types Reference

## Oppgavetyper

| Type | Code | Description | Usage |
|------|------|-------------|-------|
| `BEH_SAK_MK` | BEH_SAK_MK | Behandle sak medlemskap | Main treatment task |
| `VUR` | VUR | Vurder dokument | Document evaluation |
| `JFR` | JFR | Journalføring | Filing incoming documents |
| `BEH_SED` | BEH_SED | Behandle SED | EU document handling |
| `VURD_HENV` | VURD_HENV | Vurder henvendelse | Evaluate inquiry |
| `VURD_MAN_INNB` | VURD_MAN_INNB | Manglende innbetaling | Missing payment follow-up |
| `BEH_ARSAVREG` | BEH_ARSAVREG | Årsavregning | Annual settlement |

## Type Detection Methods

The `Oppgave` domain class provides helper methods:

```kotlin
oppgave.erBehandling()              // BEH_SAK_MK
oppgave.erJournalFøring()           // JFR
oppgave.erSedBehandling()           // BEH_SED
oppgave.erVurderDokument()          // VUR
oppgave.erVurderHenvendelse()       // VURD_HENV
oppgave.erManglendeInnbetalingBehandling()  // VURD_MAN_INNB
oppgave.erÅrsavregning()            // BEH_ARSAVREG
```

## Task Status

| Status | Description | Is Active |
|--------|-------------|-----------|
| `OPPRETTET` | Created, not started | Yes |
| `AAPNET` | Opened by saksbehandler | Yes |
| `UNDER_BEHANDLING` | Being worked on | Yes |
| `FERDIGSTILT` | Completed | No |
| `FEILREGISTRERT` | Registered in error | No |

```kotlin
// Check if task is active
oppgave.erAktiv()  // true if OPPRETTET, AAPNET, or UNDER_BEHANDLING
```

## Tema

| Tema | Code | Description |
|------|------|-------------|
| `MED` | MED | Medlemskap |
| `UFM` | UFM | Unntak fra medlemskap |
| `TRY` | TRY | Trygdeavgift |

## Behandlingstema Codes

Extensive list of 25+ behandlingstema codes:

### EU/EØS Related

| Code | Description |
|------|-------------|
| `ab0424` | EU_EOS_LAND |
| `ab0483` | EU_EOS_YRKESAKTIV |
| `ab0480` | EU_EOS_PENSJONIST_ELLER_UFORETRYGDET |
| `ab0481` | EU_EOS_IKKE_YRKESAKTIV |
| `ab0482` | EU_EOS_NORGE_ER_UTPEKT_SOM_LOVVALGSLAND |

### Bilateral Treaties

| Code | Description |
|------|-------------|
| `ab0387` | AVTALELAND |
| `ab0477` | AVTALAND_YRKESAKTIV |
| `ab0476` | AVTALAND_PENSJONIST_ELLER_UFORETRYGDET |

### Outside Agreements

| Code | Description |
|------|-------------|
| `ab0388` | UTENFOR_AVTALELAND |
| `ab0484` | UTENFOR_AVTALAND_YRKESAKTIV |

### Exception Handling

| Code | Description |
|------|-------------|
| `ab0460` | ANMODNING_UNNTAK |
| `ab0461` | REGISTRERING_UNNTAK |

## Priority

| Priority | Code | Description |
|----------|------|-------------|
| `LAV` | LAV | Low priority |
| `NORM` | NORM | Normal (default) |
| `HOY` | HOY | High priority |

## Task Type Selection Logic

The `OppgavetypeUtleder` determines type based on:

```kotlin
fun utledOppgavetype(behandling: Behandling): Oppgavetyper {
    return when {
        behandling.type == ÅRSAVREGNING -> BEH_ARSAVREG
        behandling.erSedBehandling() -> BEH_SED
        behandling.tema == TRYGDEAVGIFT -> VURD_MAN_INNB
        behandling.harVurderDokument() -> VUR
        else -> BEH_SAK_MK
    }
}
```

## Special Rules

### ÅRSAVREGNING
- Always creates new task (no reuse)
- Uses `BEH_ARSAVREG` type
- Different priority handling

### VUR + TRY Filtering
Tasks with `VUR` type and `TRY` tema are filtered out in searches:
- These are gift-tax related, not membership
- Intentional exclusion from membership task lists

### SED Tasks
- Uses `BEH_SED` type
- Created when behandling initiated from SED receipt
- Links to journalpost with mottakskanal=EESSI
