# DVH Tables (Data Warehouse Export)

Tables used for exporting data to NAV's data warehouse for statistics and reporting.

## Overview

These tables are populated by scheduled jobs that extract relevant data from the main domain tables for downstream analytics.

## FAGSAK_DVH

Denormalized case data for reporting.

```sql
CREATE TABLE fagsak_dvh (
    saksnummer      VARCHAR2(99) NOT NULL,
    fagsak_type     VARCHAR2(99),
    tema            VARCHAR2(20),
    status          VARCHAR2(99),
    opprettet_dato  TIMESTAMP(6),
    avsluttet_dato  TIMESTAMP(6),
    bruker_id       VARCHAR2(99),
    -- Additional denormalized fields
    CONSTRAINT pk_fagsak_dvh PRIMARY KEY (saksnummer)
);
```

## BEHANDLING_DVH

Denormalized treatment data.

```sql
CREATE TABLE behandling_dvh (
    id                  NUMBER NOT NULL,
    saksnummer          VARCHAR2(99),
    beh_type            VARCHAR2(99),
    beh_tema            VARCHAR2(99),
    status              VARCHAR2(99),
    opprettet_dato      TIMESTAMP(6),
    avsluttet_dato      TIMESTAMP(6),
    behandlingstid_dager NUMBER,
    resultat_type       VARCHAR2(99),
    -- Additional fields
    CONSTRAINT pk_behandling_dvh PRIMARY KEY (id)
);
```

## AKTOR_DVH

Denormalized actor data.

```sql
CREATE TABLE aktor_dvh (
    id              NUMBER NOT NULL,
    saksnummer      VARCHAR2(99),
    rolle           VARCHAR2(99),
    aktor_type      VARCHAR2(99),    -- PERSON, ORGANISASJON, etc.
    -- Additional fields
    CONSTRAINT pk_aktor_dvh PRIMARY KEY (id)
);
```

## FEILLOGG_DVH

Error logging for DVH sync issues.

```sql
CREATE TABLE feillogg_dvh (
    id              NUMBER NOT NULL,
    saksnummer      VARCHAR2(99),
    feilmelding     VARCHAR2(4000),
    feil_tidspunkt  TIMESTAMP(6),
    -- Additional fields
    CONSTRAINT pk_feillogg_dvh PRIMARY KEY (id)
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

### Average processing time by treatment type
```sql
SELECT beh_type, AVG(behandlingstid_dager) as snitt_dager
FROM behandling_dvh
WHERE avsluttet_dato IS NOT NULL
GROUP BY beh_type
ORDER BY snitt_dager DESC;
```

### Cases opened per month
```sql
SELECT TO_CHAR(opprettet_dato, 'YYYY-MM') as mnd, COUNT(*) as antall
FROM fagsak_dvh
GROUP BY TO_CHAR(opprettet_dato, 'YYYY-MM')
ORDER BY mnd;
```

## Note on Migrations

DVH table migrations use decimal versioning (V17.1, V17.2, etc.) separate from main schema migrations which use integer versioning.

Location: `app/src/main/resources/db/migration/melosysDB/`
