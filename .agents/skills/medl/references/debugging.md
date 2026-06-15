# MEDL Debugging Guide

## Common SQL Queries

### Find MEDL Period ID for a Behandling

```sql
-- Lovvalgsperiode (table lovvalg_periode; FK beh_resultat_id; MEDL id medlperiode_id)
SELECT lp.id, lp.medlperiode_id, lp.fom_dato, lp.tom_dato, lp.lovvalgsland, lp.lovvalg_bestemmelse
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingsresultatId;

-- Medlemskapsperiode (table medlemskapsperiode; FK behandlingsresultat_id; MEDL id medlperiode_id)
SELECT mp.id, mp.medlperiode_id, mp.fom_dato, mp.tom_dato, mp.medlemskapstype
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingsresultatId;

-- Anmodningsperiode (table anmodningsperiode; FK beh_resultat_id; MEDL id medlperiode_id)
SELECT ap.id, ap.medlperiode_id, ap.fom_dato, ap.tom_dato, ap.sendt_utland
FROM anmodningsperiode ap
WHERE ap.beh_resultat_id = :behandlingsresultatId;
```

### Find Periods Missing MEDL ID

```sql
-- Lovvalgsperioder that should have MEDL ID but don't.
-- Joins: behandlingsresultat.behandling_id = behandling.id; behandling.saksnummer = fagsak.saksnummer
SELECT lp.id, b.beh_type, b.status, f.saksnummer
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON br.behandling_id = lp.beh_resultat_id
JOIN behandling b ON b.id = br.behandling_id
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE lp.medlperiode_id IS NULL
AND lp.innvilgelse_resultat = 'INNVILGET'
AND b.status = 'AVSLUTTET';
```

### Check Prosessinstans for MEDL Steps

```sql
-- Columns: prosess_type (entity field `type`), sist_fullfort_steg (field `sistFullførtSteg`)
SELECT pi.uuid, pi.prosess_type, pi.status, pi.sist_fullfort_steg, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.registrert_dato DESC;
```

### Find Failed MEDL Steps

```sql
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.sist_fullfort_steg IN (
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
val medl = medlemskapClient.hentPeriode(stored.toString())  // MedlemskapClient.hentPeriode is public
// Compare medl.sporingsinformasjon.versjon
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
SELECT lp.lovvalg_bestemmelse, lp.tillegg_bestemmelse
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingsresultatId;
```

**Resolution**:
- Add mapping in `MedlPeriodeKonverter` (`lovvalgsbestemmelseTilGrunnlagMedlTabell`)
- Or fix incorrect bestemmelse on period

### Issue: Article 13 Wrong Status (Provisional vs Final)

**Symptom**: Period created as FORL when should be ENDL (or vice versa)

**Key factors**:
1. Is it Article 13? (`harBestemmelse(ART_13_*)`)
2. Is exemption registration approved? (`erGodkjentRegistreringUnntak`)

**Investigation**:
```sql
SELECT br.resultat_type, br.utfall_registrering_unntak,
       lp.lovvalg_bestemmelse, lp.innvilgelse_resultat
FROM behandlingsresultat br
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingsresultatId;
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

**Cause**: The bestemmelse is `FO_883_2004_ART87_8` or `FO_883_2004_ART87A`, which requires
overgangsregelbestemmelser. These are NOT stored on `lovvalg_periode` — `MedlService` reads them
from the SED grunnlag (`SedGrunnlag.overgangsregelbestemmelser` on the behandling's
mottatte opplysninger). If that list is empty, the FunksjonellException is thrown.

**Investigation**:
```sql
-- Confirm the bestemmelse on the period
SELECT lp.lovvalg_bestemmelse, lp.tillegg_bestemmelse
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingsresultatId;
-- Then inspect the mottatte opplysninger (SedGrunnlag) for the behandling for overgangsregelbestemmelser.
```

**Resolution**: Ensure the SED grunnlag carries the overgangsregelbestemmelser for the behandling.

## Log Patterns

### MEDL REST Calls
```bash
# All MEDL calls
grep "MedlemskapClient" application.log

# Specific behandling
grep "MedlemskapClient" application.log | grep "<behandlingId>"

# Errors only
grep "MedlemskapClient" application.log | grep -i "error\|exception\|feilet"
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
| REST Client | `integrasjon/.../medl/MedlemskapClient.kt` |
| Core Service | `integrasjon/.../medl/MedlService.kt` |
| Converter | `integrasjon/.../medl/MedlPeriodeKonverter.kt` |
| DTOs | `integrasjon/.../medl/api/v1/` |
| Period Service | `service/.../medl/MedlPeriodeService.java` |
| Membership Step | `saksflyt/.../steg/medl/LagreMedlemsperiodeMedl.kt` |
| Lovvalg Step | `saksflyt/.../steg/medl/LagreLovvalgsperiodeMedl.kt` |
| Anmodning Step | `saksflyt/.../steg/medl/LagreAnmodningsperiodeIMedl.java` |

## Retry Configuration

`MedlemskapClient` is an `open class` annotated with Spring `@Retryable` (methods are `open` so the
retry proxy works):
- Automatic retry on transient failures
- Check Spring Retry logs for retry attempts

## Manual MEDL Operations

If saga fails, manual operations via service:

```kotlin
// Inject MedlService
@Autowired lateinit var medlService: MedlService

// Search existing periods (public; returns a Saksopplysning wrapping the MEDL response)
val saksopplysning = medlService.hentPeriodeListe(fnr, fom, tom)

// Note: hentEksisterendePeriode(medlPeriodeID) is PRIVATE in MedlService.
// To fetch a single period directly, inject MedlemskapClient and call hentPeriode(periodeId).

// Create new period (fnr, periodeMedBestemmelse, kildedokumenttypeMedl) -> returns medlPeriodeID
val nyPeriodeId = medlService.opprettPeriodeEndelig(fnr, periodeMedBestemmelse, KildedokumenttypeMedl.SED)

// Update period (periodeMedBestemmelse, kildedokumenttypeMedl)
medlService.oppdaterPeriodeEndelig(periodeMedBestemmelse, KildedokumenttypeMedl.SED)
