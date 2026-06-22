# Trygdeavgift Beregning (Calculation)

## Overview

Trygdeavgift calculation is performed by the external `melosys-trygdeavgift-beregning` service
and coordinated by `TrygdeavgiftsberegningService` in melosys-api.

## Calculation Inputs

### Required Data

1. **Avgiftspliktig periode** (Medlemskapsperiode or Lovvalgsperiode)
   - Period dates (fom/tom)
   - Trygdedekning (coverage type)

2. **Skatteforhold til Norge**
   - Skatteplikttype: SKATTEPLIKTIG or IKKE_SKATTEPLIKTIG (whether the person is skattepliktig til Norge)

3. **Inntektsperiode**
   - Avgiftspliktig månedsinntekt
   - Avgiftspliktig totalinntekt (used by the 25% rule)
   - Arbeidsgiveravgift betales til skatt flag

### Avgiftsdekning Types

| Type | Description | Typical Use |
|------|-------------|-------------|
| `HELSEDEL_UTEN_SYKEPENGER` | Health coverage without sick pay | Pensjonister |
| `HELSEDEL_MED_SYKEPENGER` | Health coverage with sick pay | Yrkesaktive |
| `PENSJONSDEL_UTEN_YRKESSKADETRYGD` | Pension without occupational injury | Standard |
| `PENSJONSDEL_MED_YRKESSKADETRYGD` | Pension with occupational injury | Special cases |

## Avgiftssatser (Rates)

Rates are defined annually in regulations. The rate depends on:

- Year of the period
- Avgiftsdekning
- Member type (frivillig/pliktig)
- Skatteforhold

### Example Rate Structure (Conceptual)
```
Satser 2024:
├── Frivillig medlemskap
│   ├── Helsedel uten sykepenger: X.X%
│   ├── Helsedel med sykepenger: Y.Y%
│   ├── Pensjonsdel uten yrkesskadetrygd: Z.Z%
│   └── Pensjonsdel med yrkesskadetrygd: W.W%
└── Pliktig medlemskap (same structure)
```

## Calculation Formula

### Basic Calculation
```
månedlig_avgift = avgiftspliktig_månedsinntekt * (sats / 100)
```

### Period Calculation
```
total_avgift = sum(månedlig_avgift for each month in period)
```

### Rounding
- Amounts are rounded down to nearest whole krone
- Same principle as Skatteetaten uses

## 25% Rule (25-prosentregelen)

### Purpose
Hjemmel: folketrygdloven § 23-3 fjerde ledd. Caps the avgift at 25% of the part of income that
**exceeds the minstebeløp** (not 25% of total income). It applies to pliktig medlemskap, frivillig
medlemskap and EØS-pensjonister — applied differently per group (e.g. frivillig: helsedel/pensjonsdel
handled separately; misjonær has its own rule).

### Implementation
The cap is **not** computed in melosys-api. It is enforced in the external
`melosys-trygdeavgift-beregning` service (constant `MAKS_PROSENTDEL_AV_INNTEKT = 0.25` in
`MaksimalAvgift.kt`). In this repo, `TotalbeløpBeregner` only aggregates avgift and inntekt across
periods for årsavregning (`hentTotalavgift`, `hentTotalinntekt`); it contains no 0.25 cap or
minstebeløp logic.

## Minstebeløp (Minimum Amount)

Hjemmel: folketrygdloven § 23-3 fjerde ledd. A minimum threshold exists:
- The 25% cap applies to income above the minstebeløp
- Threshold defined per year in the external beregning service

## Special Cases

### Skattepliktig til Norge
When member pays regular tax to Norway:
- Trygdeavgift may already be collected via tax system
- Check `skalBetalesTilNav()` in TrygdeavgiftMottakerService

### Arbeidsgiveravgift betales til Skatt
When employer pays arbeidsgiveravgift to Skatteetaten:
- Affects calculation basis
- Different handling for utenlandske arbeidsgivere

### EØS Pensjonist
Special handling for EEA pensioners:
- Uses `EøsPensjonistTrygdeavgiftsberegningService`
- Different rate structure
- HelseutgiftDekkesPeriode as basis

### Satsendring
When rates change mid-year:
- Periods must be split at rate change boundary
- New calculation performed for each rate period
- Handled by `SatsendringProsessGenerator`

## Code Locations

### Main Services
- `TrygdeavgiftsberegningService`: Main calculation coordinator
- `EøsPensjonistTrygdeavgiftsberegningService`: EØS pensionist specific
- `TrygdeavgiftsberegningMapping`: Maps domain to beregning module DTOs

### Validation
- `TrygdeavgiftsberegningValidator`: Validates input before calculation
- `EøsPensjonistTrygdeavgiftsberegningValidator`: EØS specific validation

### Models
- `TrygdeavgiftsgrunnlagModel`: Input model for calculation
- `InntektsperiodeModel`: Income period model
- `SkatteforholdTilNorgeModel`: Tax relationship model

## Integration with Beregningsmodul

External service `melosys-trygdeavgift-beregning`:
- REST API call from TrygdeavgiftsberegningService
- Returns calculated amounts per period
- Handles rate lookup and formula application

## Debugging Calculation Issues

### Check Input Data
```sql
-- skatteforhold_til_norge and inntektsperiode have no FK to medlemskapsperiode;
-- they are linked through trygdeavgiftsperiode (skatteforhold_id / inntektsperiode_id).
SELECT mp.id, mp.trygde_dekning,
       s.skatteplikt_type,
       i.avgiftspliktig_inntekt_mnd_verdi,
       i.avgiftspliktig_inntekt_total_verdi,
       i.aga_betales_til_skatt
FROM medlemskapsperiode mp
JOIN trygdeavgiftsperiode t ON t.medlemskapsperiode_id = mp.id
LEFT JOIN skatteforhold_til_norge s ON t.skatteforhold_id = s.id
LEFT JOIN inntektsperiode i ON t.inntektsperiode_id = i.id
WHERE mp.behandlingsresultat_id = :id;
```

### Verify Calculated Result
```sql
SELECT t.periode_fra, t.periode_til,
       t.trygdesats, t.trygdeavgift_beloep_mnd_verdi
FROM trygdeavgiftsperiode t
WHERE t.medlemskapsperiode_id = :medlemskapsperiodeId;
```

## Related Confluence Pages

- [Beregning/fastsettelse av trygdeavgift](https://confluence.adeo.no/spaces/TEESSI/pages/544312761)
- [25-prosentregelen og minstebeløpet](https://confluence.adeo.no/spaces/TEESSI/pages/704156896)
- [Avgiftsforskrifter og satser](https://confluence.adeo.no/spaces/TEESSI/pages/566069299)
