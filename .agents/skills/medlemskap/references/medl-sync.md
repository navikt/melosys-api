# MEDL Synchronization Reference

## Overview

Medlemskapsperioder are synchronized to MEDL (Medlemskapsregister) when vedtak is made.
The sync happens via the `LAGRE_MEDLEMSKAPSPERIODE_MEDL` saksflyt step.

## Sync Flow

```
fattVedtak() → IVERKSETT_VEDTAK_FTRL prosessinstans
                        │
                        ▼
              LAGRE_MEDLEMSKAPSPERIODE_MEDL steg
                        │
            ┌───────────┴───────────┐
            ▼                       ▼
      First time?               Re-assessment?
            │                       │
            ▼                       ▼
      opprettPeriodeEndelig()  erstattMedlemskapsperioder()
            │                       │
            ▼                       ▼
      medlPeriodeID set        Original periods updated/terminated
```

## When MEDL Sync Occurs

### LagreMedlemsperiodeMedl Step

**Skips when**:
- `behandling.erEøsPensjonist()` - EØS pensioners don't create FTRL periods
- `fagsak.erLovvalg()` - Lovvalg cases use LagreLovvalgsperiodeMedl instead

**Executes for**:
- FTRL sakstype (FTRL cases)
- Non-EØS pensioner behandlingstema

## MEDL Operations

### Create Period (First-time)

```kotlin
// Via MedlPeriodeService.opprettPeriodeEndelig()
// Creates period in MEDL with status GYLD (valid)
// Returns medlPeriodeID which is stored on Medlemskapsperiode
```

**MEDL Request**:
```json
{
  "ident": "12345678901",
  "fraOgMed": "2024-01-01",
  "tilOgMed": "2024-12-31",
  "status": "GYLD",
  "grunnlag": "FTRL_2_8",
  "dekning": "FULL"
}
```

### Update Period

```kotlin
// Via MedlPeriodeService.oppdaterPeriodeEndelig()
// Updates existing period identified by medlPeriodeID
// Requires version for optimistic locking
```

### Terminate Period (Opphør)

```kotlin
// Via MedlPeriodeService.opprettOpphørtPeriode() or oppdaterOpphørtPeriode()
// Sets status to AVST (ceased/terminated)
```

### Void Period (Feilregistrert)

```kotlin
// Via MedlPeriodeService.avvisPeriodeFeilregistrert()
// Sets statusårsak to FEILREGISTRERT
```

## Re-assessment Sync Logic

When `erstattMedlemskapsperioder()` is called:

### Step 1: Terminate Non-Continued INNVILGET Periods
```kotlin
opprinneligeGjeldendeMedlemskapsperioder
    .filter { it.erInnvilget() }
    .filterNot { eksistererMedlemskapsperiodeMedID(perioderSomVidereføres, it.medlPeriodeID) }
    .forEach { medlPeriodeService.avvisPeriodeOpphørt(it.hentMedlPeriodeID()) }
```

### Step 2: Void Non-Continued OPPHØRT Periods
```kotlin
opprinneligeGjeldendeMedlemskapsperioder
    .filter { it.erOpphørt() }
    .filterNot { eksistererMedlemskapsperiodeMedID(perioderSomVidereføres, it.medlPeriodeID) }
    .forEach { medlPeriodeService.avvisPeriodeFeilregistrert(it.hentMedlPeriodeID()) }
```

### Step 3: Create/Update INNVILGET Periods
```kotlin
nyeMedlemskapsperioder
    .filter { it.erInnvilget() }
    .forEach { opprettEllerOppdaterMedlPeriode(behandlingID, it) }
```

### Step 4: Create/Update OPPHØRT Periods
```kotlin
nyeMedlemskapsperioder
    .filter { it.erOpphørt() }
    .forEach { opprettEllerOppdaterOpphørtMedlPeriode(behandlingID, it) }
```

## medlPeriodeID Management

### When Set

- Set after successful `opprettPeriode*()` call
- MEDL returns the ID in response
- Stored on Medlemskapsperiode entity

### When NULL

- Period not yet synced to MEDL
- Vedtak not yet iverksatt
- MEDL sync failed (check prosessinstans status)

### Inheritance in Re-assessment

- New periods carry `medlPeriodeID` from original when updating
- New periods without matching original get NULL (new MEDL period created)

## Debugging MEDL Sync

### Check Prosessinstans Status

```sql
SELECT pi.id, pi.type, pi.status, pi.sist_utforte_steg, pi.feil_melding
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%';
```

### Check Period MEDL Status

```sql
SELECT
    mp.id,
    mp.fom_dato,
    mp.tom_dato,
    mp.innvilgelse_resultat,
    mp.medlperiode_id,
    CASE
        WHEN mp.medlperiode_id IS NULL THEN 'NOT_SYNCED'
        ELSE 'SYNCED'
    END as medl_status
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingsresultatId;
```

### Verify MEDL Contents

```kotlin
// Via MedlService
val perioder = medlService.hentPerioder(fnr, fom, tom)
val periode = medlService.hentEksisterendePeriode(medlPeriodeID)
```

## Common Sync Issues

### 1. Null medlPeriodeID After Sync

**Symptom**: Period shows `medlperiode_id IS NULL` after vedtak

**Causes**:
- MEDL API returned null ID
- Sync step failed
- Prosessinstans stuck

**Investigation**:
```sql
-- Check prosessinstans
SELECT * FROM prosessinstans
WHERE behandling_id = :id
AND type = 'IVERKSETT_VEDTAK_FTRL';

-- Check step status
SELECT sist_utforte_steg, feil_melding FROM prosessinstans
WHERE behandling_id = :id;
```

### 2. Version Conflict on Update

**Symptom**: 409 Conflict error

**Cause**: Stale version in update request

**Solution**: Fetch current period before update to get latest version

### 3. Skip Conditions Not Met

**Symptom**: Period not synced even though it should be

**Check skip conditions**:
```kotlin
// These cause sync to be skipped
behandling.erEøsPensjonist()  // → true skips
fagsak.erLovvalg()            // → true skips (use LagreLovvalgsperiodeMedl)
```

## MEDL Data Mapping

### Grunnlag Mapping

| Melosys Bestemmelse | MEDL Grunnlag |
|---------------------|---------------|
| FTRL_KAP2_2_1 | FTRL_2_1 |
| FTRL_KAP2_2_5_* | FTRL_2_5_* |
| FTRL_KAP2_2_8 | FTRL_2_8 |
| TILLEGGSAVTALE_NATO | NATO |

### Dekning Mapping

| Melosys Trygdedekning | MEDL Dekning |
|-----------------------|--------------|
| FULL_DEKNING | FULL |
| HELSEDEL_MED_SYKEPENGER | HELSEDEL_MED_SYKE |
| HELSEDEL_UTEN_SYKEPENGER | HELSEDEL_UTEN_SYKE |

## Related Skills

- **medl**: Detailed MEDL integration patterns
- **saksflyt**: Saga step execution details
- **vedtak**: Vedtak iverksetting flow
