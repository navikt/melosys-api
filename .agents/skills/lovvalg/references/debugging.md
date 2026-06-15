# Lovvalg Debugging Guide

## Schema notes (verified against Flyway migrations)

- Table is **`lovvalg_periode`**, FK to behandlingsresultat is **`beh_resultat_id`**.
- `behandlingsresultat` PK is **`behandling_id`** (no `id` column); it equals `behandling.id`,
  so `lovvalg_periode.beh_resultat_id` can be joined straight to `behandling.id`.
- `behandling` references `fagsak` via **`saksnummer`** (not `fagsak_id`); `fagsak`'s PK is `saksnummer`.
- Real `lovvalg_periode` columns: `fom_dato`, `tom_dato`, `lovvalgsland`, `lovvalg_bestemmelse`,
  `tillegg_bestemmelse`, `unntak_fra_lovvalgsland`, `unntak_fra_bestemmelse`,
  `innvilgelse_resultat`, `medlemskapstype`, `trygde_dekning`, `medlperiode_id`, `kilde`.
- There are **no `buc_case`, `sed` or `dokumentproduksjon` tables**. BUC/SED state lives in RINA,
  queried via EUX (`EessiService` / `EessiClient`), not the DB. The saksrelasjon linking a behandling/
  arkivsak to a RINA case is owned by melosys-eessi (via `EessiClient`), not the melosys-api DB.
- `behandling`'s status column is **`status`** (the kodeverk table is `behandling_status`).
- Unntak (exception) is **not** a separate table — it is stored as the `unntak_fra_*` columns above.

## Common Investigation Queries

### Find Lovvalgsperioder for a Fagsak
```sql
SELECT lp.*, b.status
FROM lovvalg_periode lp
JOIN behandling b ON lp.beh_resultat_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
ORDER BY lp.fom_dato DESC;
```

### Check Lovvalgsbestemmelse and Period Details
```sql
SELECT lp.id, lp.fom_dato, lp.tom_dato,
       lp.lovvalgsland, lp.lovvalg_bestemmelse,
       lp.tillegg_bestemmelse, lp.innvilgelse_resultat,
       lp.medlemskapstype, lp.trygde_dekning
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingId;
```

### Inspect a registered unntak (exception)
```sql
-- Unntak is stored as columns on lovvalg_periode
SELECT lp.id, lp.fom_dato, lp.tom_dato,
       lp.unntak_fra_lovvalgsland, lp.unntak_fra_bestemmelse,
       lp.lovvalg_bestemmelse, lp.innvilgelse_resultat
FROM lovvalg_periode lp
JOIN behandling b ON lp.beh_resultat_id = b.id
WHERE b.saksnummer = :saksnummer
AND (lp.unntak_fra_bestemmelse IS NOT NULL OR lp.unntak_fra_lovvalgsland IS NOT NULL);
```

### Check the iverksett-vedtak saga (A1 / SED / MEDL steps)
```sql
-- A1 sending, SED sending and MEDL registration are saga steps, not DB rows.
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.endret_dato DESC;
```

### Check MEDL-registered Lovvalgsperioder
```sql
SELECT lp.medlperiode_id, lp.fom_dato, lp.tom_dato,
       lp.lovvalgsland, lp.lovvalg_bestemmelse, b.status
FROM lovvalg_periode lp
JOIN behandling b ON lp.beh_resultat_id = b.id
WHERE b.saksnummer = :saksnummer
AND lp.medlperiode_id IS NOT NULL;
```

## Common Issues and Solutions

### Issue: Wrong Article Selected

**Symptoms:**
- Incorrect lovvalgsbestemmelse on lovvalgsperiode
- Vedtak with wrong article reference

**Investigation:**
1. Check the avklarte fakta / vilkårsvurdering for the behandling
2. Verify the regelmodul output that picked the article
3. Inspect the resulting `lovvalg_periode.lovvalg_bestemmelse`

```sql
-- Inspect the persisted bestemmelse for the behandling
SELECT lp.id, lp.lovvalg_bestemmelse, lp.tillegg_bestemmelse, lp.lovvalgsland
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingId;
```

### Issue: A1 Not Generated

**Symptoms:**
- No A1 document in Joark
- The iverksett-vedtak saga stopped before sending the A1

**Investigation:**
1. Check the IVERKSETT_VEDTAK saga status
2. Verify which step (`sist_fullfort_steg`) the prosessinstans stopped on
3. Check dokgen / A1 brev mapping (`A1Mapper`, `BrevDataByggerA1`)

```sql
-- Check vedtak prosessinstans (no separate dokumentproduksjon table exists)
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%';
```

### Issue: SED Not Sent to EUX

**Symptoms:**
- BUC created but SED not in RINA
- EUX errors in logs

**Investigation:**
1. Check EUX logs (`EessiClient` / `EessiService`)
2. Verify BUC and SED status directly in RINA (not in the DB)
3. Check the prosessinstans `sist_fullfort_steg` for the relevant SED-sending step

```sql
-- SED/BUC state lives in RINA, not the melosys-api DB. From the DB side you can
-- only inspect the saga step that should have sent the SED.
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId;
```

### Issue: Lovvalgsland Wrong

**Symptoms:**
- Wrong country in lovvalgsperiode
- A1 shows incorrect applicable legislation

**Investigation:**
1. Check the søknad / avklarte fakta arbeidsland data for the behandling
2. Verify arbeidsgiver/foretak land (from EREG/AAREG lookups)
3. Check any SED mapping

```sql
-- Inspect the persisted lovvalgsland
SELECT lp.id, lp.lovvalgsland, lp.lovvalg_bestemmelse, lp.fom_dato, lp.tom_dato
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingId;
```

### Issue: Period Overlap Validation Error

**Symptoms:**
- Cannot save lovvalgsperiode
- Validation error about overlapping periods

**Investigation:**
1. Check existing lovvalgsperioder for the fagsak
2. Verify dates don't overlap

```sql
-- Find overlapping periods across avsluttede behandlinger on the same fagsak
SELECT lp1.id as id1, lp1.fom_dato as fom1, lp1.tom_dato as tom1,
       lp2.id as id2, lp2.fom_dato as fom2, lp2.tom_dato as tom2
FROM lovvalg_periode lp1
JOIN behandling b1 ON lp1.beh_resultat_id = b1.id
JOIN behandling b2 ON b1.saksnummer = b2.saksnummer
JOIN lovvalg_periode lp2 ON lp2.beh_resultat_id = b2.id
WHERE b1.saksnummer = :saksnummer
AND b1.status = 'AVSLUTTET'
AND b2.status = 'AVSLUTTET'
AND lp1.id < lp2.id
AND lp1.fom_dato <= COALESCE(lp2.tom_dato, DATE '9999-12-31')
AND COALESCE(lp1.tom_dato, DATE '9999-12-31') >= lp2.fom_dato;
```

### Issue: Missing MEDL Registration

**Symptoms:**
- Lovvalgsperiode not in MEDL
- `medlperiode_id` is NULL

**Investigation:**
1. Check prosessinstans for the LAGRE_LOVVALGSPERIODE_MEDL step
2. Verify MEDL integration errors
3. Check lovvalgsperiode has required fields

```sql
-- Check the vedtak saga step (prosessinstans has no feilmelding column;
-- failed steps surface via the status column; step history lives in prosessinstans_hendelser)
SELECT pi.uuid, pi.sist_fullfort_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%';

-- Check lovvalgsperiode fields
SELECT lp.id, lp.lovvalg_bestemmelse, lp.innvilgelse_resultat,
       lp.lovvalgsland, lp.medlperiode_id
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingId;
```

## Log Patterns to Search

### Lovvalg Processing
```
grep "LovvalgsperiodeService" app.log
grep "LovvalgsbestemmelseService" app.log
```

### A1 / Brev Generation
```
grep "A1Mapper" app.log
grep "BrevDataByggerA1" app.log
```

### EUX/SED Issues
```
grep "EessiClient" app.log | grep -i "error"
grep "EessiService" app.log
grep "SedMottakRuting" app.log
grep "LA_BUC" app.log
```

### MEDL Integration
```
grep "LagreLovvalgsperiodeMedl" app.log
grep "MedlPeriodeService" app.log
```

## Key Classes for Debugging

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `LovvalgsperiodeService` | Period management | `lagreLovvalgsperioder()` |
| `LovvalgsbestemmelseService` | Article mapping | `hentLovvalgsbestemmelser()` |
| `EosVedtakService` | EOS vedtak | `fattVedtak()` |
| `A1Mapper` / `BrevDataByggerA1` | A1 brev mapping | (brev mapping) |
| `EessiService` / `EessiClient` | EUX/RINA integration | `opprettOgSendSed()`, `opprettBucOgSed()` |
| `LagreLovvalgsperiodeMedl` | MEDL registration | (saga step) |
