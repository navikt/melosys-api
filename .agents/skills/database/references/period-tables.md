# Period Tables

All period types in melosys-api, stored as children of BEHANDLINGSRESULTAT.

## Period Hierarchy

```
BEHANDLINGSRESULTAT
├── LOVVALG_PERIODE          - Applicable legislation periods
├── MEDLEMSKAPSPERIODE       - Membership periods
├── ANMODNINGSPERIODE        - Exception request periods (Art. 16)
├── UTPEKINGSPERIODE         - Designation periods (Art. 13)
└── TRYGDEAVGIFTSPERIODE     - Social security charge periods
    ├── links to INNTEKTSPERIODE
    ├── links to SKATTEFORHOLD_TIL_NORGE
    └── links to HELSEUTGIFT_DEKKES_PERIODE
```

## LOVVALG_PERIODE

Determines which country's social security legislation applies.

```sql
CREATE TABLE lovvalg_periode (
    id                  NUMBER NOT NULL PRIMARY KEY,
    beh_resultat_id     NUMBER NOT NULL REFERENCES behandlingsresultat(behandling_id),
    fom_dato            DATE NOT NULL,
    tom_dato            DATE,                       -- NULL = open-ended
    lovvalgsland        VARCHAR2(99),               -- Country code (ISO 3166-1 alpha-2)
    lovvalg_bestemmelse VARCHAR2(99),               -- Legal article (e.g., "ART_12")
    tillegg_bestemmelse VARCHAR2(99),               -- Additional article
    innvilgelse_resultat VARCHAR2(99) NOT NULL,     -- INNVILGET, AVSLÅTT, etc.
    medlemskapstype     VARCHAR2(99),               -- PLIKTIG, FRIVILLIG
    trygde_dekning      VARCHAR2(99),               -- Coverage type
    medlperiode_id      NUMBER                      -- MEDL external reference
);
```

**Common queries:**
```sql
-- Find lovvalg periods for behandling
SELECT * FROM lovvalg_periode
WHERE beh_resultat_id = :behandlingId
ORDER BY fom_dato;

-- Find active lovvalg at a date
SELECT * FROM lovvalg_periode
WHERE beh_resultat_id = :behandlingId
AND fom_dato <= :dato
AND (tom_dato IS NULL OR tom_dato >= :dato);
```

## MEDLEMSKAPSPERIODE

Membership periods in Norwegian National Insurance (folketrygden).

```sql
CREATE TABLE medlemskapsperiode (
    id                      NUMBER NOT NULL PRIMARY KEY,
    behandlingsresultat_id  NUMBER NOT NULL REFERENCES behandlingsresultat(behandling_id),
    fom_dato                DATE NOT NULL,
    tom_dato                DATE,
    innvilgelse_resultat    VARCHAR2(99) NOT NULL,
    medlemskapstype         VARCHAR2(99) NOT NULL,  -- PLIKTIG, FRIVILLIG
    trygde_dekning          VARCHAR2(99) NOT NULL,  -- Full/partial coverage
    bestemmelse             VARCHAR2(99) NOT NULL,  -- Legal basis
    medlperiode_id          NUMBER                  -- MEDL external ID (synced to MEDL register)
);
```

**Note:** `medlperiode_id` is the external reference to the MEDL register. When populated, this period has been synced to MEDL.

## ANMODNINGSPERIODE

Exception request periods for Article 16 applications (request for exception from normal rules).

```sql
CREATE TABLE anmodningsperiode (
    id                      NUMBER NOT NULL PRIMARY KEY,
    beh_resultat_id         NUMBER NOT NULL REFERENCES behandlingsresultat(behandling_id),
    fom_dato                DATE NOT NULL,
    tom_dato                DATE,
    lovvalgsland            VARCHAR2(99),           -- Requested country
    lovvalg_bestemmelse     VARCHAR2(99),
    tillegg_bestemmelse     VARCHAR2(99),
    unntak_fra_lovvalgsland VARCHAR2(99),           -- Exception from which country
    unntak_fra_bestemmelse  VARCHAR2(99),
    trygde_dekning          VARCHAR2(99),
    medlperiode_id          NUMBER,
    sendt_utland            NUMBER,                 -- 1 = sent to foreign authority
    anmodet_av              VARCHAR2(30)            -- Who requested (NO/foreign country)
);
```

## UTPEKINGSPERIODE

Designation periods for Article 13 cases (working in multiple countries).

```sql
CREATE TABLE utpekingsperiode (
    id              NUMBER NOT NULL PRIMARY KEY,
    beh_resultat_id NUMBER NOT NULL REFERENCES behandlingsresultat(behandling_id),
    fom_dato        DATE NOT NULL,
    tom_dato        DATE,
    utpekt_land     VARCHAR2(99),                   -- Designated country
    bestemmelse     VARCHAR2(99)                    -- Legal basis
);
```

## TRYGDEAVGIFTSPERIODE

Social security charge calculation periods (for voluntary membership).

```sql
CREATE TABLE trygdeavgiftsperiode (
    id                          NUMBER NOT NULL PRIMARY KEY,
    periode_fra                 DATE NOT NULL,
    periode_til                 DATE NOT NULL,
    trygdeavgift_beloep_mnd_verdi   NUMBER NOT NULL,
    trygdeavgift_beloep_mnd_valuta  VARCHAR2(3) NOT NULL,   -- Currency code
    trygdesats                  NUMBER NOT NULL,            -- Percentage rate
    inntektsperiode_id          NUMBER REFERENCES inntektsperiode(id),
    medlemskapsperiode_id       NUMBER REFERENCES medlemskapsperiode(id),
    skatteforhold_id            NUMBER REFERENCES skatteforhold_til_norge(id),
    helseutgift_dekkes_periode_id NUMBER REFERENCES helseutgift_dekkes_periode(id),
    lovvalg_periode_id          NUMBER REFERENCES lovvalg_periode(id)
);
```

## Supporting Period Tables

### INNTEKTSPERIODE

Income periods used for charge calculation.

```sql
CREATE TABLE inntektsperiode (
    id              NUMBER NOT NULL PRIMARY KEY,
    fom_dato        DATE NOT NULL,
    tom_dato        DATE NOT NULL,
    inntekt_verdi   NUMBER NOT NULL,
    inntekt_valuta  VARCHAR2(3) NOT NULL
);
```

### SKATTEFORHOLD_TIL_NORGE

Tax relation to Norway (affects charge calculation).

```sql
CREATE TABLE skatteforhold_til_norge (
    id                  NUMBER NOT NULL PRIMARY KEY,
    fom_dato            DATE NOT NULL,
    tom_dato            DATE NOT NULL,
    skattepliktig       NUMBER(1) NOT NULL          -- 1 = tax liable to Norway
);
```

### HELSEUTGIFT_DEKKES_PERIODE

Health expense coverage periods.

```sql
CREATE TABLE helseutgift_dekkes_periode (
    id              NUMBER NOT NULL PRIMARY KEY,
    beh_resultat_id NUMBER NOT NULL REFERENCES behandlingsresultat(behandling_id),
    fom_dato        DATE NOT NULL,
    tom_dato        DATE NOT NULL,
    dekkes          NUMBER(1) NOT NULL              -- 1 = covered
);
```

## Common Period Queries

### Get all periods for a behandling
```sql
SELECT 'LOVVALG' as type, id, fom_dato, tom_dato, lovvalgsland as land
FROM lovvalg_periode WHERE beh_resultat_id = :behId
UNION ALL
SELECT 'MEDLEMSKAP', id, fom_dato, tom_dato, medlemskapstype
FROM medlemskapsperiode WHERE behandlingsresultat_id = :behId
UNION ALL
SELECT 'ANMODNING', id, fom_dato, tom_dato, lovvalgsland
FROM anmodningsperiode WHERE beh_resultat_id = :behId
ORDER BY fom_dato;
```

### Find overlapping periods
```sql
SELECT p1.id, p2.id, p1.fom_dato, p1.tom_dato
FROM lovvalg_periode p1
JOIN lovvalg_periode p2 ON p1.beh_resultat_id = p2.beh_resultat_id
WHERE p1.id < p2.id
AND p1.fom_dato <= COALESCE(p2.tom_dato, DATE '9999-12-31')
AND COALESCE(p1.tom_dato, DATE '9999-12-31') >= p2.fom_dato;
```

### Calculate total trygdeavgift for a case
```sql
SELECT SUM(trygdeavgift_beloep_mnd_verdi *
    MONTHS_BETWEEN(periode_til, periode_fra)) as total
FROM trygdeavgiftsperiode tp
JOIN medlemskapsperiode mp ON tp.medlemskapsperiode_id = mp.id
WHERE mp.behandlingsresultat_id = :behId;
```
