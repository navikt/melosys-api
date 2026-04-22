# Core Tables

Detailed schema for the main domain tables.

## FAGSAK

Primary case entity representing a social security case.

```sql
CREATE TABLE fagsak (
    saksnummer           VARCHAR2(99) NOT NULL PRIMARY KEY,
    gsak_saksnummer      NUMBER,
    fagsak_type          VARCHAR2(99) NOT NULL REFERENCES fagsak_type(kode),
    tema                 VARCHAR2(20) NOT NULL REFERENCES fagsak_tema(kode),
    status               VARCHAR2(99) NOT NULL REFERENCES fagsak_status(kode),
    betalingsvalg        VARCHAR2(99),
    registrert_dato      TIMESTAMP(6) NOT NULL,
    endret_dato          TIMESTAMP(6) NOT NULL,
    registrert_av        VARCHAR2(99),
    endret_av            VARCHAR2(99)
);
```

**Key relationships:**
- Has many AKTOER (actors)
- Has many BEHANDLING (treatments)
- Has many KONTAKTOPPLYSNING (contacts)

## BEHANDLING

Processing instance within a case.

```sql
CREATE TABLE behandling (
    id                              NUMBER NOT NULL PRIMARY KEY,
    saksnummer                      VARCHAR2(99) NOT NULL REFERENCES fagsak(saksnummer),
    status                          VARCHAR2(99) NOT NULL REFERENCES behandling_status(kode),
    beh_type                        VARCHAR2(99) NOT NULL REFERENCES behandling_type(kode),
    beh_tema                        VARCHAR2(99) NOT NULL REFERENCES behandling_tema(kode),
    behandlingsfrist                DATE NOT NULL,
    oppgave_id                      VARCHAR2(10),
    initierende_journalpost_id      VARCHAR2(99),
    initierende_dokument_id         VARCHAR2(99),
    opprinnelig_behandling_id       NUMBER REFERENCES behandling(id),
    siste_opplysninger_hentet_dato  TIMESTAMP(6),
    dokumentasjon_svarfrist_dato    TIMESTAMP(6),
    registrert_dato                 TIMESTAMP(6) NOT NULL,
    endret_dato                     TIMESTAMP(6) NOT NULL,
    registrert_av                   VARCHAR2(99),
    endret_av                       VARCHAR2(99)
);
```

**Key relationships:**
- Belongs to FAGSAK
- Has one BEHANDLINGSRESULTAT
- Has many PROSESSINSTANS
- Has many SAKSOPPLYSNING
- Has many MOTTATTEOPPLYSNINGER
- Has many BEHANDLINGSNOTAT
- Has many BEHANDLINGSAARSAK
- Can reference parent BEHANDLING (self-referential for ny vurdering)

## AKTOER

Parties involved in a case.

```sql
CREATE TABLE aktoer (
    id                      NUMBER NOT NULL PRIMARY KEY,
    saksnummer              VARCHAR2(99) NOT NULL REFERENCES fagsak(saksnummer),
    rolle                   VARCHAR2(99) NOT NULL REFERENCES rolle_type(kode),
    aktoer_id               VARCHAR2(99),          -- NAV actor ID (for BRUKER)
    person_ident            VARCHAR2(11),          -- Norwegian ID (fnr/dnr)
    orgnr                   VARCHAR2(99),          -- Organization number
    eu_eos_institusjon_id   VARCHAR2(99),          -- EU/EEA institution
    trygdemyndighet_land    VARCHAR2(2),           -- Country code
    utenlandsk_person_id    VARCHAR2(99),          -- Foreign person ID
    registrert_dato         TIMESTAMP(6) NOT NULL,
    endret_dato             TIMESTAMP(6) NOT NULL,
    registrert_av           VARCHAR2(99),
    endret_av               VARCHAR2(99)
);
```

**Role types (ROLLE_TYPE):**
- `BRUKER` - Citizen the case concerns
- `VIRKSOMHET` - Company
- `ARBEIDSGIVER` - Employer
- `TRYGDEMYNDIGHET` - Foreign social security authority
- `FULLMEKTIG` - Representative/Power of attorney
- `REPRESENTANT` - Representative
- `REPRESENTANT_TRYGDEAVGIFT` - Social security charge payer

## BEHANDLINGSRESULTAT

Outcome of a treatment. One-to-one with BEHANDLING.

```sql
CREATE TABLE behandlingsresultat (
    behandling_id               NUMBER NOT NULL PRIMARY KEY REFERENCES behandling(id),
    behandlingsmaate            VARCHAR2(99) NOT NULL REFERENCES behandlingsmaate(kode),
    resultat_type               VARCHAR2(99) REFERENCES behandlingsresultat_type(kode),
    fastsatt_av_land            VARCHAR2(99),
    begrunnelse_fritekst        VARCHAR2(4000),
    innledning_fritekst         VARCHAR2(4000),
    ny_vurdering_bakgrunn       VARCHAR2(4000),
    trygdeavgift_fritekst       VARCHAR2(4000),
    utfall_registrering_unntak  VARCHAR2(40),
    utfall_utpeking             VARCHAR2(40),
    fakturaserie_referanse      VARCHAR2(40),
    trygdeavgift_type           VARCHAR2(99),
    registrert_dato             TIMESTAMP(6) NOT NULL,
    endret_dato                 TIMESTAMP(6) NOT NULL,
    registrert_av               VARCHAR2(99),
    endret_av                   VARCHAR2(99)
);
```

**Key relationships:**
- Belongs to BEHANDLING (1:1)
- Has many LOVVALG_PERIODE
- Has many MEDLEMSKAPSPERIODE
- Has many ANMODNINGSPERIODE
- Has many UTPEKINGSPERIODE
- Has many TRYGDEAVGIFTSPERIODE
- Has one VEDTAK_METADATA
- Has many VILKAARSRESULTAT
- Has many AVKLARTEFAKTA
- Has many KONTROLLRESULTAT
- Has many BEHANDLINGSRES_BEGRUNNELSE

## PROSESSINSTANS

Saga/workflow instance for orchestrating complex processes.

```sql
CREATE TABLE prosessinstans (
    uuid                RAW(16) NOT NULL PRIMARY KEY,
    prosess_type        VARCHAR2(99) NOT NULL REFERENCES prosess_type(kode),
    behandling_id       NUMBER REFERENCES behandling(id),
    status              VARCHAR2(20) NOT NULL,
    sist_fullfort_steg  VARCHAR2(99) REFERENCES prosess_steg(kode),
    data                CLOB,                   -- JSON state data
    sed_laas_referanse  VARCHAR2(99),
    registrert_dato     TIMESTAMP(6) NOT NULL,
    endret_dato         TIMESTAMP(6) NOT NULL
);

-- Prosess status values: STARTET, FULLFØRT, FEILET, AVBRUTT
```

**Key relationships:**
- Belongs to BEHANDLING (optional)
- References PROSESS_TYPE and PROSESS_STEG

## SAKSOPPLYSNING

Stored case data (JSON documents).

```sql
CREATE TABLE saksopplysning (
    id              NUMBER NOT NULL PRIMARY KEY,
    behandling_id   NUMBER NOT NULL REFERENCES behandling(id),
    opplysning_type VARCHAR2(99) NOT NULL REFERENCES saksopplysning_type(kode),
    versjon         VARCHAR2(99) NOT NULL,
    dokument        CLOB NOT NULL,              -- JSON data
    registrert_dato TIMESTAMP(6) NOT NULL,
    endret_dato     TIMESTAMP(6) NOT NULL
);
```

**Key relationships:**
- Belongs to BEHANDLING
- Has many SAKSOPPLYSNING_KILDE

## MOTTATTEOPPLYSNINGER

Received documents/information.

```sql
CREATE TABLE mottatteopplysninger (
    id                  NUMBER NOT NULL PRIMARY KEY,
    behandling_id       NUMBER NOT NULL REFERENCES behandling(id),
    type                VARCHAR2(100) REFERENCES mottatteopplysninger_type(kode),
    versjon             VARCHAR2(10) NOT NULL,
    data                CLOB NOT NULL,          -- JSON data
    original_data       CLOB,                   -- Original received data
    mottaksdato         DATE,
    ekstern_referanse_id VARCHAR2(99),
    registrert_dato     TIMESTAMP(6) NOT NULL,
    endret_dato         TIMESTAMP(6) NOT NULL
);
```

## Useful Queries

### Find all data for a case
```sql
SELECT
    f.saksnummer, f.fagsak_type, f.tema, f.status as fagsak_status,
    b.id as behandling_id, b.beh_type, b.beh_tema, b.status as beh_status,
    br.resultat_type, br.behandlingsmaate
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE f.saksnummer = 'MEL-12345'
ORDER BY b.registrert_dato DESC;
```

### Find active prosessinstanser for behandling
```sql
SELECT p.uuid, p.prosess_type, p.status, p.sist_fullfort_steg, p.data
FROM prosessinstans p
WHERE p.behandling_id = :behandlingId
AND p.status = 'STARTET'
ORDER BY p.registrert_dato DESC;
```

### Get saksopplysninger for behandling
```sql
SELECT id, opplysning_type, versjon, dokument
FROM saksopplysning
WHERE behandling_id = :behandlingId
ORDER BY opplysning_type;
```
