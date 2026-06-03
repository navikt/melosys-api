---
name: behandling
description: |
  Expert knowledge of Behandling (case treatment) domain in melosys-api.
  Use when: (1) Understanding the behandling lifecycle and status transitions,
  (2) Debugging issues with behandling creation, status changes, or closure,
  (3) Understanding relationships between Fagsak, Behandling, and Behandlingsresultat,
  (4) Investigating why a behandling is stuck or has wrong status,
  (5) Understanding Behandlingstyper, Behandlingstema, Behandlingsstatus enums,
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
├── saksnummer: String (PK — no numeric id)
├── type: Sakstyper (EU_EOS, FTRL, TRYGDEAVTALE)
├── tema: Sakstemaer
├── status: Saksstatuser
└── behandlinger: List<Behandling>  (linked via saksnummer FK)

    Behandling (Treatment)
    ├── id: Long
    ├── status: Behandlingsstatus
    ├── type: Behandlingstyper
    ├── tema: Behandlingstema
    ├── behandlingsfrist: LocalDate
    ├── registrertDato / endretDato: Instant
    └── behandlingsresultat: Behandlingsresultat

        Behandlingsresultat  (PK is behandling_id — no separate id)
        ├── type: Behandlingsresultattyper
        ├── medlemskapsperioder: List<Medlemskapsperiode>
        ├── lovvalgsperioder: List<Lovvalgsperiode>
        └── vedtakMetadata: VedtakMetadata
```

### Key Enums

| Enum | Values | Description |
|------|--------|-------------|
| **Behandlingsstatus** | OPPRETTET, UNDER_BEHANDLING, AVSLUTTET, IVERKSETTER_VEDTAK, MIDLERTIDIG_LOVVALGSBESLUTNING, AVVENT_DOK_UTL, AVVENT_DOK_PART | Current processing state |
| **Behandlingstyper** | FØRSTEGANG, NY_VURDERING, HENVENDELSE, KLAGE, ENDRET_PERIODE, MANGLENDE_INNBETALING_TRYGDEAVGIFT, ÅRSAVREGNING, SATSENDRING | Type of processing |
| **Behandlingstema** | YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UTSENDT_ARBEIDSTAKER, ARBEID_FLERE_LAND, etc. | Subject matter |

### Behandling Lifecycle

```
OPPRETTET → UNDER_BEHANDLING → IVERKSETTER_VEDTAK → AVSLUTTET
                   │                                    ↑
                   ├── AVVENT_DOK_UTL ─────────────────┤
                   ├── AVVENT_DOK_PART ────────────────┤
                   └── MIDLERTIDIG_LOVVALGSBESLUTNING

MIDLERTIDIG_LOVVALGSBESLUTNING counts as INACTIVE (erInaktiv()) and is
not editable; it does not flow straight to AVSLUTTET like the AVVENT_* states.
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
// From journalføring/digital søknad to an existing sak.
// opprettNyVurdering is a private helper inside the saksflyt step
// HåndterEksisterendeSakDigitalSøknad; it ultimately calls
// BehandlingService.nyBehandling(fagsak, status, type, tema, ...).
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
behandling.erInaktiv()      // AVSLUTTET or MIDLERTIDIG_LOVVALGSBESLUTNING
behandling.erAktiv()        // !erInaktiv() (everything except those two)
behandling.erAvsluttet()    // AVSLUTTET
behandling.erRedigerbar()   // erAktiv() && status != IVERKSETTER_VEDTAK
                            //   && !(ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV)
behandling.erVenterForDokumentasjon()  // AVVENT_DOK_PART, AVVENT_DOK_UTL, ANMODNING_UNNTAK_SENDT
```

## Service Layer

### BehandlingService
Location: `service/src/main/java/no/nav/melosys/service/behandling/BehandlingService.java`

Key operations:
- `hentBehandling(behandlingId)` - Get by ID
- `nyBehandling(fagsak, status, type, tema, ...)` - Create a new behandling
- `endreStatus(behandling, status)` / `endreStatus(behandlingID, status)` - Change status
- `endreBehandling(behandlingID, nyType, nyTema, nyStatus, nyMottaksdato)` - Edit type/tema/status/frist
- `avsluttBehandling(behandlingId)` / `avsluttBehandling(behandlingId, behandlingsresultattype: Behandlingsresultattyper)` - Close treatment

### BehandlingsresultatService
Location: `service/src/main/kotlin/no/nav/melosys/service/behandling/BehandlingsresultatService.kt`

Key operations:
- `hentBehandlingsresultat(behandlingsid)` - Get result
- `lagre(resultat)` - Save result
- `lagreNyttBehandlingsresultat(behandling)` - Create a fresh (IKKE_FASTSATT) result for a behandling
- `oppdaterBehandlingsresultattype(id, type)` - Set the result type

## Debugging Queries

### Find Behandling by ID
```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.id = :behandlingId;
```

### Find Active Behandlinger for Person
```sql
-- behandling links to fagsak via saksnummer; the actor (aktoer_id) lives in aktoer
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
WHERE a.aktoer_id = :aktorId
AND b.status NOT IN ('AVSLUTTET');
```

### Find Stuck Behandlinger
```sql
SELECT b.id, b.status, b.endret_dato, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '2' HOUR;
```

### Check Behandling with Prosessinstans
```sql
-- prosessinstans has no status column; saga progress is tracked via steg + antall_retry/sover_til
SELECT b.id, b.status, p.prosess_type, p.steg, p.antall_retry, p.sover_til
FROM behandling b
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
WHERE b.id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Stuck in IVERKSETTER_VEDTAK | Can't edit behandling | Check prosessinstans steg / antall_retry / sover_til |
| Wrong behandlingstype | Validation errors | Check journalføring logic |
| Missing behandlingsresultat | NullPointer | Check saga completed successfully |
| Can't create ny vurdering | FunksjonellException | Check fagsak.status and existing active behandling |
| Status not updating | Stale data in UI | Check audit trail, version conflicts |

## Detailed Documentation

- **[Kodeverk](references/kodeverk.md)**: All behandling-related enums and codes
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
