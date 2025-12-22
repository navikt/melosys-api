# MEDL Debugging Guide

## Common SQL Queries

### Find MEDL Period ID for a Behandling

```sql
-- Lovvalgsperiode
SELECT lp.id, lp.medl_periode_id, lp.fom, lp.tom, lp.lovvalgsland, lp.bestemmelse
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingId;

-- Medlemskapsperiode
SELECT mp.id, mp.medl_periode_id, mp.fom, mp.tom, mp.medlemskapstype
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId;

-- Anmodningsperiode
SELECT ap.id, ap.medl_periode_id, ap.fom, ap.tom, ap.sendt_utland
FROM anmodningsperiode ap
WHERE ap.behandlingsresultat_id = :behandlingId;
```

### Find Periods Missing MEDL ID

```sql
-- Lovvalgsperioder that should have MEDL ID but don't
SELECT lp.id, br.type, b.status, f.saksnummer
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON br.id = lp.behandlingsresultat_id
JOIN behandling b ON b.id = br.id
JOIN fagsak f ON f.saksnummer = b.fagsak_saksnummer
WHERE lp.medl_periode_id IS NULL
AND lp.innvilgelsesresultat = 'INNVILGET'
AND b.status = 'AVSLUTTET';
```

### Check Prosessinstans for MEDL Steps

```sql
SELECT pi.id, pi.type, pi.status, pi.sist_utforte_steg, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.registrert_dato DESC;
```

### Find Failed MEDL Steps

```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.sist_utforte_steg IN (
    'LAGRE_MEDLEMSKAPSPERIODE_MEDL',
    'LAGRE_LOVVALGSPERIODE_MEDL',
    'LAGRE_ANMODNINGSPERIODE_MEDL'
)
AND pi.status = 'FEILET'
ORDER BY pi.endret_dato DESC;
```

## Common Issues

### Issue: Null medlPeriodeID After Creation

**Symptom**:
```
TekniskException: Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID
```

**Investigation**:
1. Check MEDL API logs for the request
2. Verify person exists in MEDL (by FNR)
3. Check if period already exists (duplicate)

**Resolution**:
- Manual re-run of saga step
- Check MEDL directly for existing period

### Issue: Version Conflict on Update

**Symptom**:
- 409 Conflict from MEDL API
- Optimistic locking failure

**Cause**: Stale version number in PUT request

**Investigation**:
```kotlin
// Check stored version vs MEDL version
val stored = lovvalgsperiode.medlPeriodeID
val medl = medlService.hentEksisterendePeriode(stored)
// Compare sporingsinformasjon.versjon
```

**Resolution**:
- Fetch current period before update
- Retry with correct version

### Issue: Unsupported Bestemmelse

**Symptom**:
```
TekniskException: Lovvalgsbestemmelse støttes ikke i MEDL: <bestemmelse>
```

**Investigation**:
```sql
SELECT lp.bestemmelse, lp.tilleggsbestemmelse
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingId;
```

**Resolution**:
- Add mapping in `MedlPeriodeKonverter`
- Or fix incorrect bestemmelse on period

### Issue: Article 13 Wrong Status (Provisional vs Final)

**Symptom**: Period created as FORL when should be ENDL (or vice versa)

**Key factors**:
1. Is it Article 13? (`harBestemmelse(ART_13_*)`)
2. Is exemption registration approved? (`erGodkjentRegistreringUnntak`)

**Investigation**:
```sql
SELECT br.type, br.utfall_registrering_unntak,
       lp.bestemmelse, lp.innvilgelsesresultat
FROM behandlingsresultat br
JOIN lovvalgsperiode lp ON lp.behandlingsresultat_id = br.id
WHERE br.id = :behandlingId;
```

**Logic**:
```kotlin
// Provisional when: Art. 13 AND no approved exemption
val erForeløpig = erArt13 && utfallRegistreringUnntak != GODKJENT
```

### Issue: Missing Overgangsregler

**Symptom**:
```
FunksjonellException: Grunnlaget <code> og overgangsregler skal benyttes, men er tom
```

**Cause**: Art. 87.8 or 87a requires overgangsregelbestemmelser

**Investigation**:
```sql
SELECT lp.bestemmelse, lp.overgangsregelbestemmelser
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingId;
```

**Resolution**: Set correct overgangsregelbestemmelser on period

## Log Patterns

### MEDL REST Calls
```bash
# All MEDL calls
grep "MedlemskapRestConsumer" application.log

# Specific behandling
grep "MedlemskapRestConsumer" application.log | grep "<behandlingId>"

# Errors only
grep "MedlemskapRestConsumer" application.log | grep -i "error\|exception\|feilet"
```

### Saga Step Execution
```bash
# MEDL saga steps
grep "LagreMedlemsperiodeMedl\|LagreLovvalgsperiodeMedl\|LagreAnmodningsperiodeIMedl" application.log

# With correlation ID
grep "<correlationId>" application.log | grep -i "medl"
```

### Request/Response
```bash
# Look for JSON payloads
grep "MedlemskapsunntakFor" application.log
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| REST Client | `integrasjon/.../medl/MedlemskapRestConsumer.kt` |
| Core Service | `integrasjon/.../medl/MedlService.kt` |
| Converter | `integrasjon/.../medl/MedlPeriodeKonverter.kt` |
| Period Service | `service/.../medl/MedlPeriodeService.java` |
| Membership Step | `saksflyt/.../steg/medl/LagreMedlemsperiodeMedl.kt` |
| Lovvalg Step | `saksflyt/.../steg/medl/LagreLovvalgsperiodeMedl.kt` |
| Anmodning Step | `saksflyt/.../steg/medl/LagreAnmodningsperiodeIMedl.java` |

## Retry Configuration

`MedlemskapRestConsumer` uses Spring `@Retryable`:
- Automatic retry on transient failures
- Check Spring Retry logs for retry attempts

## Manual MEDL Operations

If saga fails, manual operations via service:

```kotlin
// Inject MedlService
@Autowired lateinit var medlService: MedlService

// Search existing periods
val perioder = medlService.hentPerioder(fnr, fom, tom)

// Get specific period
val periode = medlService.hentEksisterendePeriode(medlPeriodeID)

// Create new period
val nyPeriodeId = medlService.opprettPeriodeEndelig(...)

// Update period
medlService.oppdaterPeriodeEndelig(...)
```
