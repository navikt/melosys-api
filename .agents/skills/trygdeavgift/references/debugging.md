# Trygdeavgift Debugging Guide

## Common Investigation Queries

### Find All Trygdeavgiftsperioder for a Person
```sql
SELECT t.*, mp.fom_dato as mp_fra, mp.tom_dato as mp_til,
       f.saksnummer, b.id as behandling_id, b.status
FROM trygdeavgiftsperiode t
JOIN medlemskapsperiode mp ON t.medlemskapsperiode_id = mp.id
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
WHERE a.aktoer_id = :aktorId
ORDER BY t.periode_fra DESC;
```

### Find Avgift with Skatteforhold Details
```sql
SELECT t.id, t.periode_fra, t.periode_til,
       t.trygdeavgift_beloep_mnd_verdi as mnd_beloep,
       t.trygdesats,
       s.skatteplikt_type,
       i.avgiftspliktig_inntekt_mnd_verdi as inntekt,
       i.aga_betales_til_skatt
FROM trygdeavgiftsperiode t
JOIN skatteforhold_til_norge s ON t.skatteforhold_id = s.id
JOIN inntektsperiode i ON t.inntektsperiode_id = i.id
WHERE t.id = :trygdeavgiftsperiodeId;
```

### Check Fakturaserie Status
```sql
SELECT br.fakturaserie_referanse, br.behandling_id as br_id,
       b.id as behandling_id, b.status as beh_status,
       f.saksnummer, f.betalingsvalg
FROM behandlingsresultat br
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE br.fakturaserie_referanse = :referanse;
```

### Find Årsavregning History
```sql
SELECT aa.aar, aa.beregnet_avgift_belop,
       aa.manuelt_avgift_beloep, aa.endelig_avgift_valg,
       aa.tidligere_fakturert_beloep, aa.innbetalt_trygdeavgift,
       aa.til_fakturering_beloep,
       b.id as behandling_id, b.status, b.beh_type
FROM aarsavregning aa
JOIN behandlingsresultat br ON aa.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
ORDER BY aa.aar DESC, b.registrert_dato DESC;
```

### Check Manglende Innbetaling Prosess
```sql
SELECT pi.uuid, pi.prosess_type, pi.status, pi.sist_fullfort_steg,
       pi.data, pi.registrert_dato, pi.endret_dato
FROM prosessinstans pi
WHERE pi.prosess_type = 'OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING'
AND pi.data LIKE '%"fakturaserieReferanse":":referanse"%'
ORDER BY pi.registrert_dato DESC;
```

### Find Skattehendelser
Skattehendelser are not persisted in the melosys-api database. They are consumed from Kafka by
`SkattehendelserConsumer.lesSkattehendelser()` and stored in the separate melosys-skattehendelser
service — inspect them there (or on the Kafka topic), not via a melosys-api table.

## Common Issues and Solutions

### Issue: Trygdeavgift Not Calculated

**Symptoms:**
- No trygdeavgiftsperioder on behandlingsresultat
- Fakturaseriereferanse is null

**Investigation:**
1. Check that behandlingsresultat has medlemskapsperioder or lovvalgsperioder
2. Verify skatteforhold and inntektsperioder are set
3. Check TrygdeavgiftsberegningService logs for errors
4. Verify melosys-trygdeavgift-beregning service is accessible

```sql
-- Check grunnlag exists. skatteforhold_til_norge and inntektsperiode have no FK to
-- medlemskapsperiode; they are linked via trygdeavgiftsperiode (skatteforhold_id / inntektsperiode_id).
SELECT mp.id, mp.trygde_dekning,
       (SELECT COUNT(*) FROM trygdeavgiftsperiode t
        WHERE t.medlemskapsperiode_id = mp.id AND t.skatteforhold_id IS NOT NULL) as skatteforhold_count,
       (SELECT COUNT(*) FROM trygdeavgiftsperiode t
        WHERE t.medlemskapsperiode_id = mp.id AND t.inntektsperiode_id IS NOT NULL) as inntekt_count
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingsresultatId;
```

### Issue: Wrong Avgift Amount

**Symptoms:**
- Invoice amount doesn't match expected
- 25% rule not applied

**Investigation:**
1. Check satser for the year
2. Verify inntekt amounts
3. Check if 25% rule should apply
4. Verify dekning type

```sql
-- Calculate expected avgift
SELECT t.periode_fra, t.periode_til,
       i.avgiftspliktig_inntekt_mnd_verdi as inntekt,
       t.trygdesats,
       t.trygdeavgift_beloep_mnd_verdi as faktisk_beloep,
       (i.avgiftspliktig_inntekt_mnd_verdi * t.trygdesats / 100) as forventet_beloep
FROM trygdeavgiftsperiode t
JOIN inntektsperiode i ON t.inntektsperiode_id = i.id
WHERE t.medlemskapsperiode_id IN (
    SELECT id FROM medlemskapsperiode
    WHERE behandlingsresultat_id = :behandlingsresultatId
);
```

### Issue: Duplicate Invoicing

**Symptoms:**
- Multiple fakturaseriereferanser for same period
- Customer receives multiple invoices

**Investigation:**
1. Check for duplicate vedtak prosesser
2. Verify kansellering was processed
3. Check prosessinstans history

```sql
-- Find all fakturaserie for case
SELECT br.fakturaserie_referanse, b.id, b.beh_type, b.status,
       b.registrert_dato, b.endret_dato
FROM behandlingsresultat br
JOIN behandling b ON br.behandling_id = b.id
WHERE b.saksnummer = :saksnummer
AND br.fakturaserie_referanse IS NOT NULL
ORDER BY b.registrert_dato;
```

### Issue: Årsavregning Not Created

**Symptoms:**
- No årsavregning behandling for year
- Skattehendelse received but not processed

**Investigation:**
1. Check skattehendelse status
2. Verify fagsak has trygdeavgift
3. Check for active behandling blocking

```sql
-- Skattehendelser are not stored in melosys-api (see "Find Skattehendelser" above) —
-- check the melosys-skattehendelser service / Kafka instead.

-- Check for blocking active behandling
SELECT b.id, b.beh_type, b.status
FROM behandling b
WHERE b.saksnummer = :saksnummer
AND b.status NOT IN ('AVSLUTTET');
```

### Issue: OEBS Integration Failure

**Symptoms:**
- Fakturaserie created but no invoice
- FaktureringskomponentenClient errors

**Investigation:**
1. Check faktureringskomponenten logs
2. Verify fakturaserie payload
3. Check total via faktureringskomponenten API

```kotlin
// FaktureringskomponentenClient (integrasjon module) exposes:
//   lagFakturaserie, kansellerFakturaserie, oppdaterFakturaMottaker,
//   hentTotalTrygdeavgiftForPeriode, lagFaktura
faktureringskomponentenClient.hentTotalTrygdeavgiftForPeriode(...)
```

## Log Patterns to Search

### Calculation Errors
```
grep "TrygdeavgiftsberegningService" app.log | grep -i "error\|exception"
```

### Fakturering Issues
```
grep "FaktureringskomponentenClient" app.log | grep -i "error\|failed"
grep "OPPRETT_FAKTURASERIE" app.log
```

### Årsavregning Processing
```
grep "ÅrsavregningService" app.log
grep "SkattehendelserConsumer" app.log
```

## Key Classes for Debugging

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `TrygdeavgiftsberegningService` | Calculate avgift | `beregnOgLagreTrygdeavgift()` |
| `TrygdeavgiftService` | Query avgift state | `harFakturerbarTrygdeavgift()` |
| `ÅrsavregningService` | Annual reconciliation | `opprettÅrsavregning()` |
| `FaktureringskomponentenClient` | OEBS integration (integrasjon module) | `lagFakturaserie()` |
| `ManglendeFakturabetalingConsumer` | Handle unpaid | `lesManglendeFakturabetalingMelding()` |
| `SkattehendelserConsumer` | Tax events | `lesSkattehendelser()` |
