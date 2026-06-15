# Årsavregning (Annual Reconciliation)

## Overview

Årsavregning is the annual reconciliation process that compares preliminary (forskudd)
avgift paid during the year with the final calculated avgift based on actual income.

## Process Flow

```
┌─────────────────────┐
│ Skattehendelse      │  Kafka from Skatteetaten
│ received            │  (skatteoppgjør ferdig)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Automatisk opprett  │
│ årsavregning        │
│ behandling          │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Saksbehandler       │
│ vurderer grunnlag   │
│ og fastsetter avgift│
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Fatt vedtak         │
│ IVERKSETT_VEDTAK_   │
│ AARSAVREGNING       │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Send årsavregnings- │
│ faktura (+ or -)    │
└─────────────────────┘
```

## Data Model

### Årsavregning Entity
Location: `domain/src/main/kotlin/.../avgift/Årsavregning.kt`

```kotlin
@Entity
@Table(name = "aarsavregning")
class Årsavregning(
    @Id var id: Long = 0,
    // @MapsId — shares PK (behandlingsresultat_id) with Behandlingsresultat
    var behandlingsresultat: Behandlingsresultat?,
    var aar: Int,                                       // column "aar"
    var tidligereBehandlingsresultat: Behandlingsresultat?,  // @JoinColumn "tidligere_resultat_id" (object, not an Id)
    var tidligereFakturertBeloep: BigDecimal?,
    var beregnetAvgiftBelop: BigDecimal?,               // column "beregnet_avgift_belop"
    var tilFaktureringBeloep: BigDecimal?,
    var harInnbetaltTrygdeavgift: Boolean?,
    var innbetaltTrygdeavgift: BigDecimal?,
    var manueltAvgiftBeloep: BigDecimal?,
    var endeligAvgiftValg: EndeligAvgiftValg?,
    var harSkjoennsfastsattInntektsgrunnlag: Boolean = false,
)
```

### EndeligAvgiftValg
```kotlin
enum class EndeligAvgiftValg {
    OPPLYSNINGER_ENDRET,                                  // "Jeg skal gjøre endringer"
    OPPLYSNINGER_ENDRET_MED_PERIODE_FRA_AVGIFTSSYSTEMET,  // endringer med periode fra avgiftssystemet
    OPPLYSNINGER_UENDRET,                                 // "Det er ingen endringer"
    MANUELL_ENDELIG_AVGIFT,                               // "Jeg vil oppgi endelig avgift selv"
}
```

## Service Layer

### ÅrsavregningService
Location: `service/src/main/kotlin/.../avgift/aarsavregning/ÅrsavregningService.kt`

Key methods:
- `opprettÅrsavregning()` - Create new årsavregning
- `hentTidligereTrygdeavgiftsgrunnlag()` - Get previous grunnlag
- `hentNyttTrygdeavgiftsgrunnlag()` - Get new grunnlag from income
- `replikerMedlemskapsperioder()` - Copy periods for årsavregning

### SkattehendelserConsumer
Location: `service/src/main/kotlin/.../avgift/aarsavregning/SkattehendelserConsumer.kt`

Listens for Kafka messages about completed skatteoppgjør (`lesSkattehendelser`):
- Finds fagsaker with trygdeavgift for the person/year (`finnSakMedTrygdeavgift`)
- Triggers the årsavregning prosessflyt — no skattehendelse row is persisted in melosys-api
  (the skattehendelse store lives in the separate melosys-skattehendelser service)

## Grunnlag Innhenting (Basis Retrieval)

### Scenarios

| Scenario | Tidligere grunnlag | Endelig trygdeavgift |
|----------|-------------------|---------------------|
| Ingen tidligere årsavregning | From siste vedtak | From siste vedtak + ny inntekt |
| Tidligere årsavregning, ingen ny vurdering | From tidligere årsavregning | From tidligere årsavregning |
| Tidligere årsavregning med ny vurdering (likt medlemskap) | From tidligere årsavregning | From tidligere årsavregning |
| Tidligere årsavregning med ny vurdering (ulikt medlemskap) | From tidligere årsavregning | From ny vurdering (tom trygdeavgift) |

### Logic Flow
```kotlin
fun hentGjeldendeBehandlingsresultaterForÅrsavregning(
    fagsak: Fagsak,
    år: Int
): GjeldendeBehandlingsresultaterForÅrsavregning {
    // Find siste årsavregning for året
    // Find siste vedtak etter eventuell årsavregning
    // Compare medlemskapsperioder
    // Return appropriate grunnlag
}
```

## Saksflyt

### IVERKSETT_VEDTAK_AARSAVREGNING
Flow definition:
```
SEND_FAKTURA_AARSAVREGNING →
VARSLE_PENSJONSOPPTJENING →
AVSLUTT_SAK_OG_BEHANDLING →
SEND_MELDING_OM_VEDTAK
```

### SEND_FAKTURA_AARSAVREGNING Step
Location: `saksflyt/src/main/kotlin/.../steg/arsavregning/SendFakturaÅrsavregning.kt`

Calculates `tilFaktureringBeloep` via `Årsavregning.beregnTilFaktureringsBeloep()`:
```kotlin
tilFaktureringBeloep = (manueltAvgiftBeloep ?: beregnetAvgiftBelop)
    - tidligereFakturertBeloep
    - innbetaltTrygdeavgift
    + tidligereÅrsavregning?.innbetaltTrygdeavgift   // hentTidligereInnbetaltTrygdeavgift()
```

Where:
- `manueltAvgiftBeloep ?: beregnetAvgiftBelop`: manual amount if set, otherwise the calculated avgift
- `tidligereFakturertBeloep`: previously invoiced (forskudd)
- `innbetaltTrygdeavgift`: already paid this årsavregning
- the last term adds back what was paid on the previous årsavregning

## Totalbeløp Beregning

### TotalbeløpBeregner
Location: `service/src/main/kotlin/.../avgift/aarsavregning/totalbeloep/TotalbeløpBeregner.kt`

Aggregates totals for a year (no rate/cap logic):
- Sums avgift across all trygdeavgiftsperioder (`hentTotalavgift`)
- Sums inntekt across periods (`hentTotalinntekt`)
- Handles partial year periods via `AntallMdBeregner`

The 25% cap and minstebeløp are enforced in the external `melosys-trygdeavgift-beregning` service, not here.

### AntallMdBeregner
Location: `service/src/main/kotlin/.../avgift/aarsavregning/totalbeloep/AntallMdBeregner.kt`

Calculates number of months:
- Handles period spanning year boundaries
- Pro-rata for partial months

## Ikke-Skattepliktige Årsavregning

### Special Handling
Location: `service/src/main/kotlin/.../avgift/aarsavregning/ikkeskattepliktig/`

For members not required to file Norwegian tax return:
- Different trigger mechanism (batch job)
- Different grunnlag retrieval
- May require manual income entry

### Components
- `ÅrsavregningIkkeSkattepliktigeFinner`: Finds eligible cases
- `ÅrsavregningIkkeSkattepliktigeProsessGenerator`: Creates processes
- `ÅrsavregningIkkeSkattepliktigeController`: Admin API

## Debugging

### Find Årsavregning Status
```sql
SELECT aa.*, b.status, b.beh_type, f.saksnummer
FROM aarsavregning aa
JOIN behandlingsresultat br ON aa.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
ORDER BY aa.aar DESC;
```

### Check Skattehendelse
Skattehendelser are not stored in the melosys-api database — they are consumed from Kafka by
`SkattehendelserConsumer` and persisted in the separate melosys-skattehendelser service. Inspect
them there (or via Kafka), not via a melosys-api table.

### Verify Grunnlag
```sql
-- Tidligere grunnlag
SELECT t.*, mp.id as mp_id
FROM trygdeavgiftsperiode t
JOIN medlemskapsperiode mp ON t.medlemskapsperiode_id = mp.id
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
WHERE br.behandling_id = (
    SELECT aa.tidligere_resultat_id
    FROM aarsavregning aa WHERE aa.behandlingsresultat_id = :aarsavregningBehandlingsresultatId
);

-- Endelig grunnlag
SELECT t.*, mp.id as mp_id
FROM trygdeavgiftsperiode t
JOIN medlemskapsperiode mp ON t.medlemskapsperiode_id = mp.id
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
WHERE br.behandling_id = :aarsavregningBehandlingsresultatId;
```

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Årsavregning not triggered | Skattehendelse not received | Check Kafka, verify person has avgift |
| Wrong grunnlag | Complex history | Verify behandling chain manually |
| Double årsavregning | Multiple triggers | Check for duplicate skattehendelser |
| Missing tidligere beløp | Previous årsavregning not linked | Verify tidligere_resultat_id / tidligereFakturertBeloep |

## Related Confluence Pages

- [Årsavregningen](https://confluence.adeo.no/spaces/TEESSI/pages/514480537)
- [Årsavregning - manuell støtte i flyt](https://confluence.adeo.no/spaces/TEESSI/pages/704517663)
- [Automatisert årsavregningen](https://confluence.adeo.no/spaces/TEESSI/pages/603364839)
- [Akseptansekriteria - trygdeavgift og fakturering](https://confluence.adeo.no/spaces/TEESSI/pages/720918767)
