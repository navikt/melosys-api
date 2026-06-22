---
name: database
description: |
  Expert knowledge of the melosys-api Oracle database schema.
  Use when: (1) Writing SQL queries against the melosys database,
  (2) Understanding table relationships and foreign keys,
  (3) Debugging data issues by querying the database,
  (4) Understanding what data is stored where,
  (5) Creating or modifying Flyway migrations,
  (6) Mapping domain entities to database tables.
  Triggers: SQL against melosys DB, table/column lookup, FK relationships,
  "where is X stored", "what column stores X", "why is data missing",
  beh_resultat_id, behandlingsresultat_id, saksnummer, schema.
---

# Melosys Database Schema

Oracle database schema for melosys-api. Connection: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` (user: `melosys`).

## Entity-Relationship Overview

```
FAGSAK (Case)
├── AKTOER (1:N) - Actors (citizen, employer, authorities)
├── KONTAKTOPPLYSNING (1:N) - Contact information
├── FULLMAKT (via AKTOER) - Power of attorney
├── SKJEMA_SAK_MAPPING (1:N) - Bridge to melosys-skjema-api (V152)
└── BEHANDLING (1:N) - Treatments/processing instances
    ├── MOTTATTEOPPLYSNINGER (1:N) - Received documents/info
    ├── SAKSOPPLYSNING (1:N) - Case data (JSON CLOBs)
    ├── BEHANDLINGSNOTAT (1:N) - Notes
    ├── BEHANDLINGSAARSAK (1:N) - Processing reasons
    ├── TIDLIGERE_MEDLEMSPERIODE (1:N) - Previous membership periods
    ├── PROSESSINSTANS (1:N) - Saga/workflow instances
    └── BEHANDLINGSRESULTAT (1:1) - Result
        ├── LOVVALG_PERIODE (1:N) - Applicable legislation periods
        ├── MEDLEMSKAPSPERIODE (1:N) - Membership periods
        ├── ANMODNINGSPERIODE (1:N) - Exception request periods
        ├── UTPEKINGSPERIODE (1:N) - Designation periods (Art. 13)
        ├── TRYGDEAVGIFTSPERIODE (1:N) - Social security charge periods
        ├── AARSAVREGNING (1:1) - Annual reconciliation (årsavregning)
        ├── VEDTAK_METADATA (1:1) - Decision metadata
        ├── VILKAARSRESULTAT (1:N) - Condition results
        ├── AVKLARTEFAKTA (1:N) - Clarified facts
        └── KONTROLLRESULTAT (1:N) - Control results
```

> **Recent changes (V144–V154, Jan–Apr 2026):** New `skjema_sak_mapping` table
> (bridge to melosys-skjema-api). `kilde` column added to `lovvalg_periode`,
> `medlemskapsperiode`, `helseutgift_dekkes_periode`. `aarsavregning` columns
> renamed: `*_fra_avgiftssystemet` → `*innbetalt_trygdeavgift`. New PROSESS_TYPE
> for digital søknad-mottak. See [Recent Migrations](#recent-migrations) below.

## Core Tables

### FAGSAK (Case)
Primary case entity. PK: `SAKSNUMMER` (format: MEL-{sequence}).

| Column | Type | Description |
|--------|------|-------------|
| SAKSNUMMER | VARCHAR2(99) | Primary key, e.g., "MEL-12345" |
| GSAK_SAKSNUMMER | NUMBER | Archive case ID (Joark/GSAK) |
| FAGSAK_TYPE | VARCHAR2(99) | FK to FAGSAK_TYPE (EU_EOS, TRYGDEAVTALE, FTRL) |
| TEMA | VARCHAR2(20) | FK to FAGSAK_TEMA (MEDLEMSKAP_LOVVALG, UNNTAK, TRYGDEAVGIFT) |
| STATUS | VARCHAR2(99) | FK to FAGSAK_STATUS |
| BETALINGSVALG | VARCHAR2(99) | Payment choice |

### BEHANDLING (Treatment)
Processing instance within a case. PK: `ID`.

| Column | Type | Description |
|--------|------|-------------|
| ID | NUMBER | Primary key (sequence) |
| SAKSNUMMER | VARCHAR2(99) | FK to FAGSAK |
| STATUS | VARCHAR2(99) | FK to BEHANDLING_STATUS |
| BEH_TYPE | VARCHAR2(99) | FK to BEHANDLING_TYPE |
| BEH_TEMA | VARCHAR2(99) | FK to BEHANDLING_TEMA |
| BEHANDLINGSFRIST | DATE | Processing deadline |
| OPPGAVE_ID | VARCHAR2(10) | External task ID |
| INITIERENDE_JOURNALPOST_ID | VARCHAR2(99) | Initiating document ID |
| OPPRINNELIG_BEHANDLING_ID | NUMBER | FK to parent BEHANDLING (self-ref) |

### AKTOER (Actor)
Parties involved in a case. PK: `ID`.

| Column | Type | Description |
|--------|------|-------------|
| ID | NUMBER | Primary key |
| SAKSNUMMER | VARCHAR2(99) | FK to FAGSAK |
| ROLLE | VARCHAR2(99) | FK to ROLLE_TYPE (BRUKER, ARBEIDSGIVER, etc.) |
| AKTOER_ID | VARCHAR2(99) | NAV actor ID (for BRUKER) |
| PERSON_IDENT | VARCHAR2(11) | Norwegian ID number |
| ORGNR | VARCHAR2(99) | Organization number |
| EU_EOS_INSTITUSJON_ID | VARCHAR2(99) | EU/EEA institution ID |
| TRYGDEMYNDIGHET_LAND | VARCHAR2(2) | Country code for authority |

### BEHANDLINGSRESULTAT (Result)
Outcome of a treatment. PK: `BEHANDLING_ID` (1:1 with BEHANDLING).

| Column | Type | Description |
|--------|------|-------------|
| BEHANDLING_ID | NUMBER | PK + FK to BEHANDLING |
| RESULTAT_TYPE | VARCHAR2(99) | FK to BEHANDLINGSRESULTAT_TYPE |
| BEHANDLINGSMAATE | VARCHAR2(99) | Processing method |
| FASTSATT_AV_LAND | VARCHAR2(99) | Country that determined result |
| BEGRUNNELSE_FRITEKST | VARCHAR2(4000) | Justification text |
| FAKTURASERIE_REFERANSE | VARCHAR2(40) | Billing series reference |
| TRYGDEAVGIFT_TYPE | VARCHAR2(99) | Social security charge type |

## Period Tables

All periods belong to BEHANDLINGSRESULTAT via `BEH_RESULTAT_ID` — **except
`MEDLEMSKAPSPERIODE`, whose FK column is `BEHANDLINGSRESULTAT_ID`** (renamed in
V109). Both FK columns reference `BEHANDLINGSRESULTAT(BEHANDLING_ID)`.

### LOVVALG_PERIODE (Applicable Legislation Period)
| Column | Type | Description |
|--------|------|-------------|
| FOM_DATO / TOM_DATO | DATE | Period start/end |
| LOVVALGSLAND | VARCHAR2(99) | Country code |
| LOVVALG_BESTEMMELSE | VARCHAR2(99) | Legal provision |
| INNVILGELSE_RESULTAT | VARCHAR2(99) | Grant/denial result |
| MEDLEMSKAPSTYPE | VARCHAR2(99) | Membership type |
| TRYGDE_DEKNING | VARCHAR2(99) | Coverage type |

### MEDLEMSKAPSPERIODE (Membership Period)
| Column | Type | Description |
|--------|------|-------------|
| FOM_DATO / TOM_DATO | DATE | Period start/end |
| INNVILGELSE_RESULTAT | VARCHAR2(99) | Grant/denial |
| MEDLEMSKAPSTYPE | VARCHAR2(99) | Membership type |
| TRYGDE_DEKNING | VARCHAR2(99) | Coverage |
| BESTEMMELSE | VARCHAR2(99) | Legal basis |
| MEDLPERIODE_ID | NUMBER | MEDL external ID |

## Common Queries

### Find case with all actors
```sql
SELECT f.*, a.rolle, a.aktoer_id, a.orgnr, a.person_ident
FROM fagsak f
JOIN aktoer a ON a.saksnummer = f.saksnummer
WHERE f.saksnummer = 'MEL-12345';
```

### Find active treatments for person
```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer
WHERE a.rolle = 'BRUKER' AND a.person_ident = '12345678901'
AND b.status NOT IN ('AVSLUTTET');
```

### Find membership periods for case
```sql
SELECT mp.*
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.saksnummer = 'MEL-12345';
```

### Check prosessinstans status
```sql
SELECT p.*, b.saksnummer, b.status as beh_status
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
WHERE b.id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

### Find stuck treatments
```sql
SELECT b.id, b.status, b.endret_dato, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '2' HOUR;
```

## Recent Migrations

Latest migration is **V155** (tekstblokker). Changes since V143:

| V    | Date       | What |
|------|------------|------|
| V144 | 2026-01-07 | New PROSESS_STEG `LAGRE_PERSONOPPLYSNINGER` |
| V145 | 2026-01-15 | New PROSESS_STEG `OPPRETTE_AARSAVREGNING_ENDRING` (auto-opprett årsavregning tilbake i tid) |
| V146 | 2026-01-19 | `saksopplysning_kilde` adds `versjon NUMBER(19) DEFAULT 0` — optimistic locking fix for orphanRemoval race condition |
| V147 | 2026-02-16 | New PROSESS_TYPE `MELOSYS_MOTTAK_DIGITAL_SØKNAD` + 4 nye steg (HENT_SØKNADSDATA, OPPRETT_SAK_OG_BEHANDLING_SØKNAD, OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD, LAGRE_SAKSOPPLYSNINGER_SØKNAD) |
| V148 | 2026-02-16 | `kontaktopplysning.kontakt_telefon` utvidet til VARCHAR2(255) |
| V149 | 2026-02-27 | Fjerner steg `LAGRE_SAKSOPPLYSNINGER_SØKNAD` (lagring nå i OPPRETT_SAK_OG_BEHANDLING_SØKNAD) |
| V150 | 2026-03-16 | New PROSESS_STEG `AVSLUTT_AARSAVREGNINGER` (lukker åpne årsavregninger ved annullering) |
| V151 | 2026-04-16 | `helseutgift_dekkes_periode` adds `kilde VARCHAR2(30) DEFAULT 'MELOSYS' NOT NULL` |
| V152 | 2026-04-20 | **NEW TABLE** `skjema_sak_mapping` (RAW(16) skjema_id ↔ saksnummer). New PROSESS_TYPE `MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD`. Steg fra V147 omdøpt til `*_DIGITAL_SØKNAD`-varianter (HENT_DIGITAL_SØKNADSDATA, OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD, OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD) + nye `HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD`, `SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API` |
| V153 | 2026-04-23 | `aarsavregning` columns renamed: `har_trygdeavgift_fra_avgiftssystemet` → `har_innbetalt_trygdeavgift`, `trygdeavgift_fra_avgiftssystemet` → `innbetalt_trygdeavgift` |
| V154 | 2026-04-30 | `medlemskapsperiode.kilde` + `lovvalg_periode.kilde` added (VARCHAR2(30), nullable) |
| V155 | —          | **NEW TABLE** `tekstblokker` (`TEKSTBLOKK` + `TEKSTBLOKK_TAG`, reusable text blocks with tags) |

**Note on PROSESS_STEG renames (V152):** Oracle doesn't support ON UPDATE CASCADE, so the pattern is INSERT new code → UPDATE child rows (`PROSESSINSTANS.SIST_FULLFORT_STEG`, `PROSESSINSTANS_HENDELSER.STEG`) → DELETE old code. Use this same pattern for any future PROSESS_STEG rename.

## Detailed Documentation

- **[Core Tables](references/core-tables.md)**: FAGSAK, BEHANDLING, AKTOER, PROSESSINSTANS, SKJEMA_SAK_MAPPING
- **[Period Tables](references/period-tables.md)**: All period types and relationships
- **[Kodeverk Tables](references/kodeverk.md)**: Lookup/reference tables with valid values
- **[DVH Tables](references/dvh-tables.md)**: Data warehouse export tables
