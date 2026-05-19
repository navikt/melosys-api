# Membership Periods Reference

## Period Structure

### Medlemskapsperiode Entity

```kotlin
class Medlemskapsperiode : HarBestemmelse<Bestemmelse?>, AvgiftspliktigPeriode {
    var id: Long?                          // Database ID
    var behandlingsresultat: Behandlingsresultat?  // Parent reference
    var fom: LocalDate?                    // Period start (required)
    var tom: LocalDate?                    // Period end (nullable for open-ended)
    var innvilgelsesresultat: InnvilgelsesResultat?
    var medlemskapstype: Medlemskapstyper?
    var trygdedekning: Trygdedekninger?
    var bestemmelse: Bestemmelse?
    var trygdeavgiftsperioder: MutableSet<Trygdeavgiftsperiode>  // Child avgift periods
    var medlPeriodeID: Long?               // MEDL registry reference
}
```

### Required vs Optional Fields

| Field | Required | Nullable When |
|-------|----------|---------------|
| `fom` | Always | Never |
| `tom` | Usually | When bestemmelse=FTRL_KAP2_2_1 AND land=NOR only |
| `innvilgelsesresultat` | Always | Never |
| `medlemskapstype` | Always | Auto-derived from bestemmelse |
| `trygdedekning` | Always | Never |
| `bestemmelse` | Always | Never |
| `medlPeriodeID` | No | Before MEDL sync |

## Period Creation

### First-time Treatment (Førstegangsbehandling)

```kotlin
// UtledMedlemskapsperioder.lagMedlemskapsperioder()
// Creates periods based on:
// - søknadsperiode (fom/tom from application)
// - trygdedekning (coverage type)
// - mottaksdato (receipt date)
// - bestemmelse (legal provision)
// - behandlingstema (treatment subject)
```

**YRKESAKTIV/IKKE_YRKESAKTIV Logic**:
1. If søknad.fom < mottaksdato:
   - Create AVSLAATT period: søknad.fom → mottaksdato-1
   - Create INNVILGET period: mottaksdato → søknad.tom
2. Else:
   - Create single INNVILGET period: søknad.fom → søknad.tom

**PENSJONIST Logic**:
1. Only helsedel coverage allowed
2. Similar date logic as above

### Re-assessment (Andregangsbehandling)

```kotlin
// UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling()
// Creates periods based on:
// - Original periods from opprinneligBehandling
// - New søknadsperiode
// - Behandlingstype (NY_VURDERING, KLAGE, etc.)
```

**NY_VURDERING**:
- Can extend original periods
- Can modify coverage
- Creates OPPHØRT for terminated original periods

**KLAGE**:
- Re-evaluates original decision
- May restore rejected periods

## Period Operations

### Create

```kotlin
medlemskapsperiodeService.opprettMedlemskapsperiode(
    behandlingsresultatID = 123L,
    fom = LocalDate.of(2024, 1, 1),
    tom = LocalDate.of(2024, 12, 31),
    innvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
    trygdedekning = Trygdedekninger.FULL_DEKNING,
    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
)
```

**Validations**:
1. Sakstype must be FTRL
2. Bestemmelse-trygdedekning combination valid
3. tom required unless FTRL_KAP2_2_1 with only Norway
4. fom <= tom

### Update

```kotlin
medlemskapsperiodeService.oppdaterMedlemskapsperiode(
    behandlingsresultatID = 123L,
    medlemskapsperiodeID = 456L,
    fom = LocalDate.of(2024, 1, 1),
    tom = LocalDate.of(2024, 6, 30),  // Shortened
    // ... other fields
)
```

**Note**: Updates clear existing trygdeavgiftsperioder (avgift recalculated on vedtak).

### Delete

```kotlin
medlemskapsperiodeService.slettMedlemskapsperiode(
    behandlingsresultatID = 123L,
    medlemskapsperiodeID = 456L
)
```

**Constraints**: Can only delete periods on active behandling (not AVSLUTTET).

### Replace (Re-assessment)

```kotlin
medlemskapsperiodeService.erstattMedlemskapsperioder(
    behandlingID = 789L,
    opprinneligBehandlingID = 123L,
    nyeMedlemskapsperioder = listOf(...)
)
```

**Steps**:
1. Opphør original INNVILGET periods not carried forward
2. Feilregistrer original OPPHØRT periods not carried forward
3. Create/update new INNVILGET periods
4. Create/update new OPPHØRT periods

## Period Proposals

### Generation Flow

```
1. Saksbehandler selects bestemmelse in stegvelger
2. OpprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder()
3. Validates: sakstype, bestemmelse-dekning combo, vilkår
4. UtledMedlemskapsperioder generates periods
5. Periods attached to behandlingsresultat
6. Saksbehandler can adjust in UI
```

### When Proposals Already Exist

If `behandlingsresultat.medlemskapsperioder` is not empty:
- Existing periods are updated with new bestemmelse
- medlemskapstype recalculated
- No new periods created

## Period Validation Rules

### Date Rules

```kotlin
// PeriodeRegler.feilIPeriode()
fun feilIPeriode(fom: LocalDate?, tom: LocalDate?): Boolean {
    return tom != null && fom != null && tom.isBefore(fom)
}
```

### Open-ended Periods

Only allowed when:
```kotlin
val nullTilOgMedDatoErTillatt =
    Land_iso2.NO.kode in land &&   // Land includes Norway
    land.size == 1 &&               // Only Norway
    bestemmelse in listOf(
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1  // Bosatt i Norge
    )
```

### Bestemmelse-Trygdedekning Combinations

Validated via `LovligeKombinasjonerTrygdedekningBestemmelse`:

```kotlin
// Each bestemmelse has list of allowed trygdedekninger
val allowed = LovligeKombinasjonerTrygdedekningBestemmelse
    .hentGyldigeTrygdedekninger(bestemmelse, behandlingstema)

if (trygdedekning !in allowed) {
    throw FunksjonellException("Invalid combination")
}
```

## Period Helper Methods

```kotlin
// On Medlemskapsperiode
fun erInnvilget(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.INNVILGET
fun erOpphørt(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.OPPHØRT
fun erAvslaatt(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.AVSLAATT
fun erFrivillig(): Boolean = medlemskapstype == Medlemskapstyper.FRIVILLIG
fun erPliktigMedlemskap(): Boolean = medlemskapstype == Medlemskapstyper.PLIKTIG

// On Behandlingsresultat
fun harInnvilgetMedlemskapsperiodeSomOverlapperMedÅr(år: Int): Boolean
fun clearMedlemskapsperioder()
fun addMedlemskapsperiode(periode: Medlemskapsperiode)
fun removeMedlemskapsperiode(periode: Medlemskapsperiode)
```

## Database Schema

```sql
CREATE TABLE medlemskapsperiode (
    id                    NUMBER PRIMARY KEY,
    behandlingsresultat_id NUMBER NOT NULL REFERENCES behandlingsresultat(id),
    fom_dato              DATE NOT NULL,
    tom_dato              DATE,
    innvilgelse_resultat  VARCHAR2(50) NOT NULL,  -- INNVILGET, AVSLAATT, OPPHØRT
    medlemskapstype       VARCHAR2(50) NOT NULL,  -- PLIKTIG, FRIVILLIG
    trygde_dekning        VARCHAR2(100) NOT NULL,
    bestemmelse           VARCHAR2(100) NOT NULL,
    medlperiode_id        NUMBER                  -- Reference to MEDL
);

CREATE INDEX idx_mp_behandlingsres ON medlemskapsperiode(behandlingsresultat_id);
CREATE INDEX idx_mp_medlperiode ON medlemskapsperiode(medlperiode_id);
```
