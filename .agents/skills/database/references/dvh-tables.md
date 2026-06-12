# DVH Tables (Data Warehouse Export)

Tables used for exporting data to NAV's data warehouse for statistics and reporting.

## Overview

These tables are populated by scheduled jobs that extract relevant data from the main domain tables for downstream analytics.

## FAGSAK_DVH

Denormalized case data for reporting.

```sql
CREATE TABLE fagsak_dvh (
    trans_id        NUMBER NOT NULL,
    fagsak_id       VARCHAR2(99),       -- saksnummer of the source fagsak
    gsak_saksnummer NUMBER,
    fagsak_type     VARCHAR2(99),
    tema            VARCHAR2(20),
    status          VARCHAR2(99),
    registrert_tid  TIMESTAMP(6),
    funksjonell_tid TIMESTAMP(6),
    trans_tid       TIMESTAMP(6),
    registrert_av   VARCHAR2(99),
    funksjonell_av  VARCHAR2(99),
    dml_flagg       VARCHAR2(1),
    -- Additional denormalized fields
    CONSTRAINT pk_fagsak_dvh PRIMARY KEY (trans_id)
);
```

## BEHANDLING_DVH

Denormalized treatment data.

```sql
CREATE TABLE behandling_dvh (
    trans_id            NUMBER NOT NULL,
    behandling_id       NUMBER,
    fagsak_id           VARCHAR2(99),       -- saksnummer of the source fagsak
    enhet               VARCHAR2(99),
    beh_type            VARCHAR2(99),
    beh_tema            VARCHAR2(99),
    status              VARCHAR2(99),
    resultat_type       VARCHAR2(99),
    behandlingsmaate    VARCHAR2(99),
    fastsatt_av_land    VARCHAR2(99),
    registrert_tid      TIMESTAMP(6),
    funksjonell_tid     TIMESTAMP(6),
    trans_tid           TIMESTAMP(6),
    registrert_av       VARCHAR2(99),
    funksjonell_av      VARCHAR2(99),
    dml_flagg           VARCHAR2(1),
    kildetabell         VARCHAR2(99),
    -- Additional fields
    CONSTRAINT pk_behandling_dvh PRIMARY KEY (trans_id)
);
```

## AKTOR_DVH

Denormalized actor data.

```sql
CREATE TABLE aktor_dvh (
    trans_id              NUMBER NOT NULL,
    id                    NUMBER,
    aktoer_id             VARCHAR2(99),
    saksnummer            VARCHAR2(99),
    rolle                 VARCHAR2(99),
    orgnr                 VARCHAR2(99),
    eu_eos_institusjon_id VARCHAR2(99),
    utenlandsk_person_id  VARCHAR2(99),
    representerer         VARCHAR2(99),
    trygdemyndighet_land  VARCHAR2(2),
    registrert_tid        TIMESTAMP(6),
    funksjonell_tid       TIMESTAMP(6),
    trans_tid             TIMESTAMP(6),
    registrert_av         VARCHAR2(99),
    funksjonell_av        VARCHAR2(99),
    dml_flagg             VARCHAR2(1),
    -- Additional fields
    CONSTRAINT pk_aktor_dvh PRIMARY KEY (trans_id)
);
```

## FEILLOGG_DVH

Error logging for DVH sync issues.

```sql
CREATE TABLE feillogg_dvh (
    trans_id        NUMBER NOT NULL,
    trans_tid       TIMESTAMP(6),
    kilde_tabell    VARCHAR2(99),
    kilde_pk        NUMBER,
    dml_flagg       CHAR(1),
    sqlcode         VARCHAR2(99),
    sqlerrm         VARCHAR2(4000),
    CONSTRAINT pk_feillogg_dvh PRIMARY KEY (trans_id)
);
```

## DVH Query Examples

### Statistics: Cases by type and status
```sql
SELECT fagsak_type, status, COUNT(*) as antall
FROM fagsak_dvh
GROUP BY fagsak_type, status
ORDER BY fagsak_type, status;
```

### Treatments by type and result
```sql
SELECT beh_type, resultat_type, COUNT(*) as antall
FROM behandling_dvh
WHERE status = 'AVSLUTTET'
GROUP BY beh_type, resultat_type
ORDER BY antall DESC;
```

### Cases registered per month
```sql
SELECT TO_CHAR(registrert_tid, 'YYYY-MM') as mnd, COUNT(*) as antall
FROM fagsak_dvh
GROUP BY TO_CHAR(registrert_tid, 'YYYY-MM')
ORDER BY mnd;
```

## Note on Migrations

DVH table migrations use decimal versioning (V17.1, V17.2, etc.) separate from main schema migrations which use integer versioning.

Location: `app/src/main/resources/db/migration/melosysDB/`
