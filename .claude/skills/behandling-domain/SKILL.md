---
name: behandling-domain
description: |
  Expert knowledge of Behandling (case treatment) domain in melosys-api.
  Use when: (1) Understanding the behandling lifecycle and status transitions,
  (2) Debugging issues with behandling creation, status changes, or closure,
  (3) Understanding relationships between Fagsak, Behandling, and Behandlingsresultat,
  (4) Investigating why a behandling is stuck or has wrong status,
  (5) Understanding Behandlingstype, Behandlingstema, Behandlingsstatus enums,
  (6) Debugging journalføring or SED-triggered behandling creation,
  (7) Understanding behandling creation via saksflyt sagas.
---

# Behandling Domain

Behandling (case treatment) is the core domain entity in melosys-api representing an individual
processing instance within a Fagsak (case). Each behandling tracks the lifecycle of one
decision-making process from creation through to closure.

## Quick Reference

### Domain Model Hierarchy

```
Fagsak (Case)
├── saksnummer: String
├── type: Sakstype (EOS, FTRL, TRYGDEAVTALE)
├── tema: Sakstema
├── status: Saksstatuser
└── behandlinger: List<Behandling>

    Behandling (Treatment)
    ├── id: Long
    ├── status: Behandlingsstatus
    ├── type: Behandlingstype
    ├── tema: Behandlingstema
    ├── mottattDato: LocalDate
    ├── behandlingsfrist: LocalDate
    └── behandlingsresultat: Behandlingsresultat

        Behandlingsresultat
        ├── type: Behandlingsresultattyper
        ├── medlemskapsperioder: List<Medlemskapsperiode>
        ├── lovvalgsperioder: List<Lovvalgsperiode>
        └── vedtakMetadata: VedtakMetadata
```

### Key Enums

| Enum | Values | Description |
|------|--------|-------------|
| **Behandlingsstatus** | OPPRETTET, UNDER_BEHANDLING, AVSLUTTET, IVERKSETTER_VEDTAK, MIDLERTIDIG_LOVVALGSBESLUTNING, AVVENT_DOK_UTL, AVVENT_DOK_PART | Current processing state |
| **Behandlingstype** | FØRSTEGANG, NY_VURDERING, KLAGE, ENDRET_PERIODE, HENVENDELSE, MANGLENDE_INNBETALING_TRYGDEAVGIFT | Type of processing |
| **Behandlingstema** | YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UTSENDT_ARBEIDSTAKER, ARBEID_FLERE_LAND, etc. | Subject matter |

### Behandling Lifecycle

```
OPPRETTET → UNDER_BEHANDLING → IVERKSETTER_VEDTAK → AVSLUTTET
                   │                                    ↑
                   ├── AVVENT_DOK_UTL ─────────────────┤
                   ├── AVVENT_DOK_PART ────────────────┤
                   └── MIDLERTIDIG_LOVVALGSBESLUTNING ─┘
```

## Behandling Creation Paths

### 1. Journalføring (Document Registration)
```kotlin
// JFR_NY_SAK_BRUKER saga
prosessinstansService.opprettProsessinstansJournalføringNySakBruker(journalpostId, ...)
// Creates: Fagsak + Behandling + Oppgave
```

### 2. SED Reception (EESSI)
```kotlin
// REGISTRERING_UNNTAK_NY_SAK, ARBEID_FLERE_LAND_NY_SAK, etc.
sedMottakOpprettFagsakOgBehandling.utfør(prosessinstans)
// Creates behandling from incoming SED
```

### 3. Ny Vurdering (Re-evaluation)
```kotlin
// From behandlingsmeny or journalføring to existing sak
behandlingService.opprettNyVurdering(fagsak, behandlingsårsak)
```

### 4. Automatic Creation
```kotlin
// OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING
// OPPRETT_NY_BEHANDLING_AARSAVREGNING
prosessinstansService.opprettProsessinstansNyBehandlingManglendeInnbetaling(...)
```

## Status Transitions

### Legal Transitions

| From | To | Trigger |
|------|-----|---------|
| OPPRETTET | UNDER_BEHANDLING | Saksbehandler opens case |
| UNDER_BEHANDLING | IVERKSETTER_VEDTAK | fattVedtak() called |
| UNDER_BEHANDLING | AVVENT_DOK_UTL | Waiting for foreign authority |
| UNDER_BEHANDLING | AVVENT_DOK_PART | Waiting for user documents |
| UNDER_BEHANDLING | MIDLERTIDIG_LOVVALGSBESLUTNING | Art. 13 provisional decision |
| IVERKSETTER_VEDTAK | AVSLUTTET | Vedtak saga completes |
| AVVENT_* | UNDER_BEHANDLING | Documents received |
| Any active | AVSLUTTET | Manual close via behandlingsmeny |

### Status Helper Methods

```kotlin
behandling.erAktiv()        // OPPRETTET or UNDER_BEHANDLING
behandling.erInaktiv()      // Not erAktiv()
behandling.erAvsluttet()    // AVSLUTTET
behandling.erRedigerbar()   // Can be edited
behandling.erVenterForDokumentasjon()  // AVVENT_*
```

## Service Layer

### BehandlingService
Location: `service/src/main/kotlin/.../behandling/BehandlingService.kt`

Key operations:
- `hentBehandling(id)` - Get by ID
- `endreStatus(behandling, status)` - Change status
- `opprettNyVurdering(fagsak, årsak)` - Create new evaluation
- `avsluttBehandling(id, resultat)` - Close treatment

### BehandlingsresultatService
Location: `service/src/main/kotlin/.../behandling/BehandlingsresultatService.kt`

Key operations:
- `hentBehandlingsresultat(behandlingId)` - Get result
- `lagre(behandlingsresultat)` - Save result
- `oppdaterMedlemskapsperioder(...)` - Update periods

## Debugging Queries

### Find Behandling by ID
```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.id = :behandlingId;
```

### Find Active Behandlinger for Person
```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.bruker_aktor_id = :aktorId
AND b.status NOT IN ('AVSLUTTET');
```

### Find Stuck Behandlinger
```sql
SELECT b.id, b.status, b.endret_dato, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '2' HOUR;
```

### Check Behandling with Prosessinstans
```sql
SELECT b.id, b.status, p.type, p.status as prosess_status
FROM behandling b
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
WHERE b.id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Stuck in IVERKSETTER_VEDTAK | Can't edit behandling | Check prosessinstans status |
| Wrong behandlingstype | Validation errors | Check journalføring logic |
| Missing behandlingsresultat | NullPointer | Check saga completed successfully |
| Can't create ny vurdering | FunksjonellException | Check fagsak.status and existing active behandling |
| Status not updating | Stale data in UI | Check audit trail, version conflicts |

## Detailed Documentation

- **[Kodeverk](references/kodeverk.md)**: All behandling-related enums and codes
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
