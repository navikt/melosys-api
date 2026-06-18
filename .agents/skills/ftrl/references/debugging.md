# FTRL Debugging Guide

> Schema notes (verified against Flyway migrations + JPA entities):
> - `medlemskapsperiode` columns: `behandlingsresultat_id` (FK), `fom_dato`, `tom_dato`,
>   `bestemmelse`, `trygde_dekning`, `medlemskapstype`, `innvilgelse_resultat`, `medlperiode_id`.
>   There is no `arbeidsland` column (dropped in V98) and no direct
>   `skatteforhold`/`inntektsperiode` table keyed on the medlemskapsperiode.
> - `behandlingsresultat`'s PK is `behandling_id` (no separate `id`), so
>   `mp.behandlingsresultat_id = br.behandling_id = behandling.id`.
> - `behandling` and `fagsak` link via `saksnummer`. `fagsak` PK is `saksnummer`, type column
>   is `fagsak_type`, and the bruker's aktørID lives in `aktoer` (`aktoer_id`, `rolle='BRUKER'`).

## Common Investigation Queries

### Find All Medlemskapsperioder for Person
```sql
SELECT mp.*, f.saksnummer, b.status as beh_status
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
WHERE a.aktoer_id = :aktorId
AND f.fagsak_type = 'FTRL'
ORDER BY mp.fom_dato DESC;
```

### Check Bestemmelse with Full Details
```sql
SELECT mp.id, mp.fom_dato, mp.tom_dato,
       mp.bestemmelse, mp.trygde_dekning,
       mp.medlemskapstype,
       mp.innvilgelse_resultat, mp.medlperiode_id
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Check Trygdeavgiftsperioder
```sql
SELECT t.*, mp.id as mp_id
FROM trygdeavgiftsperiode t
JOIN medlemskapsperiode mp ON t.medlemskapsperiode_id = mp.id
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Find FTRL Vedtak Process
```sql
SELECT pi.uuid, pi.prosess_type, pi.status, pi.sist_fullfort_steg,
       pi.registrert_dato, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type = 'IVERKSETT_VEDTAK_FTRL'
ORDER BY pi.registrert_dato DESC;
```

### Check MEDL Registration Status
```sql
SELECT mp.id, mp.fom_dato, mp.tom_dato, mp.bestemmelse,
       mp.medlperiode_id, mp.innvilgelse_resultat
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.saksnummer = :saksnummer
AND mp.innvilgelse_resultat = 'INNVILGET';
```

## Common Issues and Solutions

### Issue: Wrong Bestemmelse Selected

**Symptoms:**
- Incorrect paragraph in vedtak letter
- Validation errors on bestemmelse

**Investigation:**
1. Check behandlingstema matches bestemmelse type
2. Verify vilkårsvurdering results
3. Check avklarte fakta for relevant answers

```sql
-- Check avklarte fakta
-- Table is avklartefakta (one word), keyed on beh_resultat_id (= behandling.id);
-- the value column is fakta (not verdi).
SELECT af.referanse, af.type, af.subjekt, af.fakta, af.begrunnelse_fritekst
FROM avklartefakta af
WHERE af.beh_resultat_id = :behandlingId;

-- Check vilkårsresultat
-- vilkaarsresultat is keyed on beh_resultat_id; oppfylt is NUMBER(1) (1 = oppfylt).
SELECT vr.vilkaar, vr.oppfylt, vr.begrunnelse_fritekst
FROM vilkaarsresultat vr
WHERE vr.beh_resultat_id = :behandlingId;
```

### Issue: Invalid Trygdedekning

**Symptoms:**
- Validation error when selecting dekning
- Cannot save medlemskapsperiode

**Investigation:**
1. Check LovligeKombinasjonerTrygdedekningBestemmelse
2. Verify bestemmelse allows the dekning
3. Check if behandlingstema restricts options

```kotlin
// Valid combinations come from LovligeKombinasjonerTrygdedekningBestemmelse:
// §2-8.1.a/b/c/d + §2-8 andre/fjerde ledd -> the FTRL_2_9_* coverage series
//   (e.g. FTRL_2_9_FØRSTE_LEDD_A_HELSE, _B_PENSJON, _C_HELSE_PENSJON, ...)
// §2-7 / §2-7a + pliktige bestemmelser -> FULL_DEKNING_FTRL
// There is no §2-8 -> FULL_DEKNING_FTRL mapping.
```

### Issue: Period Overlap

**Symptoms:**
- Cannot create new medlemskapsperiode
- Validation error about overlapping

**Investigation:**
1. Check existing periods for same fagsak
2. Look for periods with same fom/tom
3. Verify previous behandling avsluttet

```sql
-- Find overlapping periods
SELECT mp1.id, mp1.fom_dato, mp1.tom_dato, mp1.bestemmelse,
       b1.id as beh_id, b1.status
FROM medlemskapsperiode mp1
JOIN behandlingsresultat br1 ON mp1.behandlingsresultat_id = br1.behandling_id
JOIN behandling b1 ON br1.behandling_id = b1.id
WHERE b1.saksnummer = :saksnummer
AND mp1.innvilgelse_resultat = 'INNVILGET'
ORDER BY mp1.fom_dato;
```

### Issue: Missing MEDL Registration

**Symptoms:**
- medlperiode_id is NULL
- Period not visible in MEDL

**Investigation:**
1. Check prosessinstans for LAGRE_MEDLEMSKAPSPERIODE_MEDL
2. Verify MEDL consumer logs
3. Check if all required fields set

```sql
-- Check vedtak process status
-- prosessinstans columns: uuid (PK), prosess_type, status, sist_fullfort_steg.
-- There is no feilmelding column; failures are recorded in prosessinstans_hendelser.melding.
SELECT pi.sist_fullfort_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type = 'IVERKSETT_VEDTAK_FTRL';

-- Inspect process failures (stacktrace/error per steg)
SELECT h.steg, h.type, h.melding, h.registrert_dato
FROM prosessinstans_hendelser h
JOIN prosessinstans pi ON h.prosessinstans_id = pi.uuid
WHERE pi.behandling_id = :behandlingId
ORDER BY h.registrert_dato DESC;

-- Check medlemskapsperiode completeness
SELECT mp.id, mp.bestemmelse, mp.trygde_dekning,
       mp.medlemskapstype, mp.innvilgelse_resultat, mp.fom_dato, mp.tom_dato
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Issue: Avgift Not Calculated

**Symptoms:**
- No trygdeavgiftsperiode created
- Faktura not generated

**Investigation:**
1. Check skatteforhold set on medlemskapsperiode
2. Verify inntektsperioder exist
3. Check TrygdeavgiftsberegningService logs

```sql
-- Check grunnlag for avgift.
-- inntektsperiode and skatteforhold_til_norge are NOT keyed directly on the
-- medlemskapsperiode; they hang off trygdeavgiftsperiode, which carries
-- medlemskapsperiode_id, inntektsperiode_id and skatteforhold_id.
SELECT mp.id,
       COUNT(t.id)                AS avgift_count,
       COUNT(t.inntektsperiode_id) AS inntekt_count,
       COUNT(t.skatteforhold_id)   AS skatteforhold_count
FROM medlemskapsperiode mp
LEFT JOIN trygdeavgiftsperiode t ON t.medlemskapsperiode_id = mp.id
WHERE mp.behandlingsresultat_id = :behandlingId
GROUP BY mp.id;
```

### Issue: Ny Vurdering Problems

**Symptoms:**
- Cannot update existing period
- Opphør not working correctly

**Investigation:**
1. Check previous behandling is AVSLUTTET
2. Verify kopiert perioder logic
3. Check erstattMedlemskapsperioder service

```sql
-- Find all behandlinger for sak
SELECT b.id, b.beh_type, b.status, b.registrert_dato
FROM behandling b
WHERE b.saksnummer = :saksnummer
ORDER BY b.registrert_dato;

-- Inspect avklarte fakta for the behandling (find the ny-vurdering bakgrunn here).
-- The fakta/type/referanse keys are enumerated in code; grep the avklartefakta
-- enums rather than assuming a literal key.
SELECT af.referanse, af.type, af.fakta, af.begrunnelse_fritekst
FROM avklartefakta af
WHERE af.beh_resultat_id = :behandlingId;
```

## Log Patterns to Search

### FTRL Processing
```
grep "MedlemskapsperiodeService" app.log
grep "FtrlVedtakService" app.log
```

### Bestemmelse Selection
```
grep "FtrlBestemmelser" app.log
grep "VilkårForBestemmelse" app.log
```

### MEDL Integration
```
grep "LagreMedlemsperiodeMedl" app.log
grep "MedlemskapClient" app.log
```

### Avgift Issues
```
grep "TrygdeavgiftsberegningService" app.log
grep "OpprettFakturaserie" app.log
```

## Key Classes for Debugging

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `MedlemskapsperiodeService` | Period management | `opprettMedlemskapsperiode()` |
| `FtrlBestemmelser` | Valid bestemmelser | Configuration class |
| `VilkårForBestemmelse*` | Vilkår evaluation | `hentVilkår()` |
| `FtrlVedtakService` | FTRL vedtak | `fattVedtak()` |
| `LagreMedlemsperiodeMedl` | MEDL registration (saga step) | `utfør()` |
| `OpprettForslagMedlemskapsperiodeService` | Period suggestion | `opprettForslag()` |
| `UtledMedlemskapsperioder` | Period derivation | `utled()` |

## Validation Rules

### Bestemmelse-Dekning Combinations

Defined by `LovligeKombinasjonerTrygdedekningBestemmelse` (and
`PliktigeMedlemskapsbestemmelser`, which are valid for any dekning):
```kotlin
// Valid combinations (Trygdedekninger codes):
§2-8.1.a / .b / .c / .d, §2-8 andre/fjerde ledd → FTRL_2_9_* series
        (FTRL_2_9_FØRSTE_LEDD_A_HELSE, _B_PENSJON, _C_HELSE_PENSJON, ... yrkesskade-varianter)
§2-7 første/fjerde ledd → FULL_DEKNING_FTRL or FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
§2-7a                   → FULL_DEKNING_FTRL or FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
pliktige bestemmelser (§2-1, §2-2, §2-5 ...) → FULL_DEKNING_FTRL
Tilleggsavtale NATO     → TILLEGGSAVTALE_NATO_HELSEDEL
// Note: there is no §2-13 → UTEN_DEKNING entry; UTEN_DEKNING is not a key in the map.
```

### Period Validation
- fom must be before or equal to tom
- tom can be null (åpen slutt)
- No overlap with other innvilget periods on same fagsak
- Bestemmelse must match behandlingstema
