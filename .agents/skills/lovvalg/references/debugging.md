# Lovvalg Debugging Guide

## Common Investigation Queries

### Find All Lovvalgsperioder for Person
```sql
SELECT lp.*, f.saksnummer, b.status as beh_status
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.bruker_aktor_id = :aktorId
ORDER BY lp.fom DESC;
```

### Check Lovvalgsbestemmelse and Period Details
```sql
SELECT lp.id, lp.fom, lp.tom,
       lp.lovvalgsland, lp.bestemmelse,
       lp.tilleggsbestemmelse, lp.innvilgelsesresultat,
       lp.medlemskapstype, lp.dekning
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingsresultatId;
```

### Find Unntaksperiode
```sql
SELECT up.*, br.id as br_id, b.id as behandling_id
FROM unntaksperiode up
JOIN behandlingsresultat br ON up.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.fagsak_id = :fagsakId;
```

### Check LA_BUC Status
```sql
SELECT bc.id, bc.type, bc.status, bc.rina_case_id,
       bc.registrert_dato, bc.endret_dato
FROM buc_case bc
WHERE bc.fagsak_id = :fagsakId
AND bc.type LIKE 'LA_BUC%'
ORDER BY bc.registrert_dato DESC;
```

### Find SEDs for BUC
```sql
SELECT sed.id, sed.type, sed.status, sed.direction,
       sed.rina_document_id, sed.registrert_dato
FROM sed sed
WHERE sed.buc_case_id = :bucCaseId
ORDER BY sed.registrert_dato;
```

### Check A1 Document Production
```sql
SELECT dp.id, dp.type, dp.status, dp.journalpost_id,
       dp.registrert_dato, b.id as behandling_id
FROM dokumentproduksjon dp
JOIN behandling b ON dp.behandling_id = b.id
WHERE b.fagsak_id = :fagsakId
AND dp.type = 'A1'
ORDER BY dp.registrert_dato DESC;
```

### Check MEDL Lovvalgsperiode
```sql
SELECT lp.medl_periode_id, lp.fom, lp.tom,
       lp.lovvalgsland, lp.bestemmelse, b.status
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.fagsak_id = :fagsakId
AND lp.medl_periode_id IS NOT NULL;
```

## Common Issues and Solutions

### Issue: Wrong Article Selected

**Symptoms:**
- Incorrect lovvalgsbestemmelse on lovvalgsperiode
- Vedtak with wrong article reference

**Investigation:**
1. Check søknad data (arbeidsland, arbeidssituasjon)
2. Verify vilkårsvurdering results
3. Check regelmodul outputs

```sql
-- Check avklartefakta for lovvalg
SELECT af.type, af.verdi, af.registrert_dato
FROM avklarte_fakta af
WHERE af.behandling_id = :behandlingId
AND af.type IN ('ARBEIDSLAND', 'ARBEIDSSITUASJON', 'BOSATT_I_NORGE')
ORDER BY af.registrert_dato DESC;
```

### Issue: A1 Not Generated

**Symptoms:**
- No A1 document in Joark
- Missing dokumentproduksjon record

**Investigation:**
1. Check vedtak saga status
2. Verify prosessinstans completed SEND_A1 step
3. Check dokgen integration

```sql
-- Check vedtak prosessinstans
SELECT pi.id, pi.type, pi.status, pi.sist_utforte_steg
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%';

-- Check brev generation
SELECT bm.id, bm.type, bm.status, bm.journalpost_id
FROM brev_metadata bm
WHERE bm.behandling_id = :behandlingId
AND bm.type LIKE '%A1%';
```

### Issue: SED Not Sent to EUX

**Symptoms:**
- BUC created but SED not in RINA
- EUX errors in logs

**Investigation:**
1. Check EUX consumer logs
2. Verify BUC and SED status
3. Check prosessinstans for SED step

```sql
-- Check pending SED
SELECT sed.id, sed.type, sed.status, sed.rina_document_id
FROM sed sed
WHERE sed.buc_case_id = :bucCaseId
AND sed.status != 'SENT';

-- Check prosessinstans SED steps
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.sist_utforte_steg LIKE '%SED%';
```

### Issue: Lovvalgsland Wrong

**Symptoms:**
- Wrong country in lovvalgsperiode
- A1 shows incorrect applicable legislation

**Investigation:**
1. Check søknad arbeidsland data
2. Verify arbeidsgiver/foretak land
3. Check any SED mapping

```sql
-- Check søknad data
SELECT s.id, s.arbeidsland, s.bostedsland, s.type
FROM soknad s
WHERE s.fagsak_id = :fagsakId
ORDER BY s.registrert_dato DESC;

-- Check foretak land
SELECT f.organisasjonsnummer, f.navn, f.land
FROM foretak f
JOIN avklarte_fakta af ON af.foretak_id = f.id
WHERE af.behandling_id = :behandlingId;
```

### Issue: Period Overlap Validation Error

**Symptoms:**
- Cannot save lovvalgsperiode
- Validation error about overlapping periods

**Investigation:**
1. Check existing lovvalgsperioder for fagsak
2. Verify dates don't overlap

```sql
-- Find overlapping periods
SELECT lp1.id as id1, lp1.fom as fom1, lp1.tom as tom1,
       lp2.id as id2, lp2.fom as fom2, lp2.tom as tom2
FROM lovvalgsperiode lp1
JOIN behandlingsresultat br1 ON lp1.behandlingsresultat_id = br1.id
JOIN behandling b1 ON br1.behandling_id = b1.id
JOIN behandling b2 ON b1.fagsak_id = b2.fagsak_id
JOIN behandlingsresultat br2 ON br2.behandling_id = b2.id
JOIN lovvalgsperiode lp2 ON lp2.behandlingsresultat_id = br2.id
WHERE b1.fagsak_id = :fagsakId
AND b1.status = 'AVSLUTTET'
AND b2.status = 'AVSLUTTET'
AND lp1.id < lp2.id
AND lp1.fom <= COALESCE(lp2.tom, DATE '9999-12-31')
AND COALESCE(lp1.tom, DATE '9999-12-31') >= lp2.fom;
```

### Issue: Missing MEDL Registration

**Symptoms:**
- Lovvalgsperiode not in MEDL
- medl_periode_id is NULL

**Investigation:**
1. Check prosessinstans for LAGRE_LOVVALGSPERIODE_MEDL step
2. Verify MEDL integration errors
3. Check lovvalgsperiode has required fields

```sql
-- Check vedtak prosess for MEDL step
SELECT pi.id, pi.sist_utforte_steg, pi.status, pi.feilmelding
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%';

-- Check lovvalgsperiode fields
SELECT lp.id, lp.bestemmelse, lp.innvilgelsesresultat,
       lp.lovvalgsland, lp.medl_periode_id
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingsresultatId;
```

## Log Patterns to Search

### Lovvalg Processing
```
grep "LovvalgsperiodeService" app.log
grep "LovvalgsbestemmelseService" app.log
```

### A1 Generation
```
grep "A1Generator" app.log
grep "dokumentproduksjon" app.log | grep -i "a1"
```

### EUX/SED Issues
```
grep "EuxConsumer" app.log | grep -i "error"
grep "SedMottakService" app.log
grep "LA_BUC" app.log
```

### MEDL Integration
```
grep "LagreLovvalgsperiodeMedl" app.log
grep "MedlConsumer" app.log
```

## Key Classes for Debugging

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `LovvalgsperiodeService` | Period management | `lagreLovvalgsperioder()` |
| `LovvalgsbestemmelseService` | Article mapping | `hentLovvalgsbestemmelser()` |
| `EosVedtakService` | EOS vedtak | `fattVedtak()` |
| `A1Generator` | A1 creation | `generer()` |
| `EuxConsumer` | EUX integration | `sendSed()`, `opprettBuc()` |
| `LagreLovvalgsperiodeMedl` | MEDL registration | `utfør()` |
