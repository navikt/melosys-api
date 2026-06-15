# Trygdeavgift (Social Insurance Charges)

Trygdeavgift represents social insurance charges associated with membership or law choice periods.

## Overview

Trygdeavgiftsperiode entities are attached to:
- `Lovvalgsperiode` (for EU/EEA law choice cases)
- `Medlemskapsperiode` (for FTRL membership cases)
- `HelseutgiftDekkesPeriode` (for EEA pensioner health coverage)

## Trygdeavgiftsperiode Entity

```kotlin
@Entity
@Table(name = "trygdeavgiftsperiode")
class Trygdeavgiftsperiode(
    val periodeFra: LocalDate,                       // @Column periode_fra
    val periodeTil: LocalDate,                       // @Column periode_til
    val trygdeavgiftsbeløpMd: Penger,                // embedded amount
    val trygdesats: BigDecimal,                      // @Column trygdesats
    // Grunnlag references (FK columns):
    var grunnlagMedlemskapsperiode: Medlemskapsperiode? = null,        // medlemskapsperiode_id
    var grunnlagLovvalgsPeriode: Lovvalgsperiode? = null,              // lovvalg_periode_id
    var grunnlagHelseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null, // helseutgift_dekkes_periode_id
) : ErPeriode
```

## Getting Charge Periods

### From Behandlingsresultat
```kotlin
// All charge periods (from all period types)
val allePerioder = resultat.trygdeavgiftsperioder

// Only from charge-liable periods
val avgiftspliktige = resultat.finnAvgiftspliktigPerioder()
```

### Check Overlap with Tax Year
```kotlin
// Check if any approved period overlaps with year
val overlapper = resultat.harInnvilgetAvgiftspliktigPeriodeSomOverlapperMedÅr(2024)
```

### Calculate Period Bounds
```kotlin
val fom = resultat.utledAvgiftspliktigperioderFom()
val tom = resultat.utledAvgiftspliktigperioderTom()
val skattepliktype = resultat.utledSkatteplikttype()
```

## AvgiftspliktigPeriode Interface

Both `Lovvalgsperiode` and `Medlemskapsperiode` implement this interface:

```kotlin
interface AvgiftspliktigPeriode {
    val trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
    val innvilgelsesresultat: InnvilgelsesResultat

    fun getFom(): LocalDate
    fun getTom(): LocalDate?
}
```

## Clearing Charge Periods

```kotlin
// Clear from behandlingsresultat
resultat.clearTrygdeavgiftsperioder()

// Clear from health expense period
resultat.clearTrygdeavgiftsperioderHelseutgiftPeriode()
```

## Business Logic

### Which Periods are Charge-Liable?

The logic in `finnAvgiftspliktigPerioder()` depends on case type and result:

```kotlin
fun finnAvgiftspliktigPerioder(): List<AvgiftspliktigPeriode> {
    return when {
        // Lovvalg case with approved period
        fagsak.erLovvalg() && erInnvilgelse() ->
            lovvalgsperioder.filter { it.erInnvilget() }

        // Membership case
        !fagsak.erLovvalg() ->
            medlemskapsperioder.filter { it.erInnvilget() }

        else -> emptyList()
    }
}
```

### Integration with Fakturering

Charge periods are used by `faktureringskomponenten` to create invoices:

```kotlin
// In IVERKSETT_VEDTAK saga
OPPRETT_FAKTURASERIE  // Creates invoice series from trygdeavgiftsperioder
```

## Skatteplikttype

Tax liability types (from kodeverk):
- `SKATTEPLIKTIG_I_NORGE` - Tax liable in Norway
- `IKKE_SKATTEPLIKTIG_I_NORGE` - Not tax liable in Norway
- `BEGRENSET_SKATTEPLIKTIG` - Limited tax liability

## Avgiftsgrupper

Charge groups determine the rate applied:
- Different rates for different situations
- Based on income, coverage type, etc.

## Debugging

### Find charge periods for a behandling
```sql
-- Via lovvalg_periode (FK column lovvalg_periode_id)
SELECT tp.*
FROM trygdeavgiftsperiode tp
JOIN lovvalg_periode lp ON lp.id = tp.lovvalg_periode_id
WHERE lp.beh_resultat_id = :behandlingId;

-- Via medlemskapsperiode (FK column medlemskapsperiode_id)
SELECT tp.*
FROM trygdeavgiftsperiode tp
JOIN medlemskapsperiode mp ON mp.id = tp.medlemskapsperiode_id
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Find cases with charge periods in specific year
```sql
SELECT DISTINCT f.saksnummer, br.resultat_type
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
JOIN trygdeavgiftsperiode tp ON tp.lovvalg_periode_id = lp.id
WHERE EXTRACT(YEAR FROM tp.periode_fra) <= 2024
AND EXTRACT(YEAR FROM tp.periode_til) >= 2024;
```

### Find missing charge periods
```sql
-- Lovvalgsperioder with INNVILGET but no charge periods
SELECT lp.id, lp.fom_dato, lp.tom_dato, lp.innvilgelse_resultat
FROM lovvalg_periode lp
LEFT JOIN trygdeavgiftsperiode tp ON tp.lovvalg_periode_id = lp.id
WHERE lp.innvilgelse_resultat = 'INNVILGET'
AND tp.id IS NULL;
```

## Related Saga Steps

| Step | Description |
|------|-------------|
| `OPPRETT_FAKTURASERIE` | Creates invoice from charge periods |
| `SEND_FAKTURA_AARSAVREGNING` | Annual reconciliation invoice |
| `RESET_ÅPNE_ÅRSAVREGNINGER` | Resets open reconciliations |
