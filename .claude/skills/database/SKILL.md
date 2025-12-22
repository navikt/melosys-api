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
---

# Melosys Database Schema

Oracle database schema for melosys-api. Connection: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` (user: `melosys`).

## Entity-Relationship Overview

```
FAGSAK (Case)
├── AKTOER (1:N) - Actors (citizen, employer, authorities)
├── KONTAKTOPPLYSNING (1:N) - Contact information
├── FULLMAKT (via AKTOER) - Power of attorney
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
        ├── VEDTAK_METADATA (1:1) - Decision metadata
        ├── VILKAARSRESULTAT (1:N) - Condition results
        ├── AVKLARTEFAKTA (1:N) - Clarified facts
        └── KONTROLLRESULTAT (1:N) - Control results
```

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

All periods belong to BEHANDLINGSRESULTAT via `BEH_RESULTAT_ID`.

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

## Detailed Documentation

- **[Core Tables](references/core-tables.md)**: FAGSAK, BEHANDLING, AKTOER, PROSESSINSTANS
- **[Period Tables](references/period-tables.md)**: All period types and relationships
- **[Kodeverk Tables](references/kodeverk.md)**: Lookup/reference tables with valid values
- **[DVH Tables](references/dvh-tables.md)**: Data warehouse export tables
