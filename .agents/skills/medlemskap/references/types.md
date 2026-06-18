# Membership Types Reference

## Medlemskapstyper

The `Medlemskapstyper` enum has five constants: `PLIKTIG`, `FRIVILLIG`, `UNNTATT`,
`DELVIS_UNNTATT`, `IKKE_MEDLEM`. The two below are the common cases; the exempt/non-member
values are used for unntak (§§2-11 til 2-13) and negative outcomes.

### PLIKTIG (Mandatory Membership)

Persons who are **required** to be members of folketrygden based on residence or employment in Norway.

**Determining Bestemmelser** (from `PliktigeMedlemskapsbestemmelser`):

| Bestemmelse | Description | FTRL Reference |
|-------------|-------------|----------------|
| `FTRL_KAP2_2_1` | Bosatt i Norge | §2-1 |
| `FTRL_KAP2_2_2` | Arbeidstaker i Norge | §2-2 |
| `FTRL_KAP2_2_3_ANDRE_LEDD` | Sokkelarbeidere | §2-3 andre ledd |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_A` | Arbeidstaker i den norske stats tjeneste (offentlig ansatt) | §2-5 første ledd a |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_B` | Arbeider for en person i den norske stats tjeneste | §2-5 første ledd b |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_C` | I forsvarets tjeneste i utlandet | §2-5 første ledd c |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_D` | Fredskorpsdeltaker / ekspert i utviklingsland | §2-5 første ledd d |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_E` | NATOs sivile krigstidsorganer | §2-5 første ledd e |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_F` | Arbeid på norskregistrert skip | §2-5 første ledd f |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_G` | Norsk sivilt luftfartsselskap (flyvende/stasjonsbetjening) | §2-5 første ledd g |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_H` | Student i utlandet med lån/stipend fra Lånekassen | §2-5 første ledd h |
| `FTRL_KAP2_2_5_ANDRE_LEDD` | Familiemedlemmer | §2-5 andre ledd |

**Vertslandsavtaler (Host Country Agreements)**:
| Bestemmelse | Organization |
|-------------|--------------|
| `ARKTISK_RÅDS_SEKRETARIAT_ART16` | Arctic Council Secretariat |
| `DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14` | Barents Secretariat |
| `DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16` | NAMMCO |
| `TILLEGGSAVTALE_NATO` | NATO (special 10% avgift rate) |

### FRIVILLIG (Voluntary Membership)

Persons who **choose** to be members of folketrygden while abroad.

**Key Bestemmelser**:

| Bestemmelse | Description | FTRL Reference |
|-------------|-------------|----------------|
| `FTRL_KAP2_2_8` | Frivillig medlemskap generelt | §2-8 |
| `FTRL_KAP2_2_7` | Pensjonister i utlandet | §2-7 |
| `FTRL_KAP2_2_7A` | Uføretrygdede i utlandet | §2-7a |
| `FTRL_KAP2_2_15_ANDRE_LEDD` | Special case (always FRIVILLIG) | §2-15 andre ledd |

**Characteristics**:
- Requires active application (søknad)
- Subject to trygdeavgift payment (NAV collects)
- 25% rule may apply (avgift capped at 25% of income)
- Can be terminated by member

## Trygdedekninger (Coverage Types)

`Trygdedekninger` is the enum stored on `Medlemskapsperiode.trygdedekning` (DB column
`trygde_dekning`). The actual constants describe the FTRL hjemmel for the coverage package,
not the avgift split. The full enum (`no.nav.melosys.domain.kodeverk.Trygdedekninger`) includes:

| Trygdedekning | Meaning |
|---------------|---------|
| `FULL_DEKNING_FTRL` | Full coverage under folketrygdloven (helse + sykepenger + pensjon + yrkesskade) |
| `FULL_DEKNING_EOSFO` | Full coverage under EØS-forordningen |
| `FULL_DEKNING` | Full coverage (generic) |
| `UTEN_DEKNING` | No coverage |
| `FTRL_2_9_FØRSTE_LEDD_A_HELSE` | §2-9 første ledd a — helsedel |
| `FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER` | §2-9 — helse + sykepenger/foreldrepenger |
| `FTRL_2_9_FØRSTE_LEDD_B_PENSJON` | §2-9 første ledd b — pensjonsdel |
| `FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE` | §2-9 — pensjon + yrkesskade |
| `FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON` | §2-9 første ledd c — helse + pensjon |
| `FTRL_2_9_FØRSTE_LEDD_C_*` (andre/tredje ledd variants) | §2-9 c with sykepenger and/or yrkesskade added |
| `FTRL_2_9_TREDJE_LEDD_YRKESSKADE` | §2-9 tredje ledd — yrkesskade |
| `FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER` | §2-7 tredje ledd b |
| `FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER` | §2-7a andre ledd b |
| `TILLEGGSAVTALE_NATO_HELSEDEL` | NATO host-country agreement — helsedel only |
| `UNNTATT_USA_5_2_G`, `UNNTATT_CAN_7_5_B` | Treaty exception values |

The naming convention is `FTRL_<paragraf>_<ledd>_<bokstav>_<dekningsinnhold>` — e.g.
`FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON` is §2-9 første ledd bokstav c with helse + pensjon.

> **Not Trygdedekninger:** `HELSEDEL_MED_SYKEPENGER`, `HELSEDEL_UTEN_SYKEPENGER`,
> `PENSJONSDEL_MED_YRKESSKADETRYGD`, `PENSJONSDEL_UTEN_YRKESSKADETRYGD` are the
> **`Avgiftsdekning`** enum, derived from a `Trygdedekninger` value by
> `AvgiftsdekningerFraTrygdedekning` (in the integrasjon/trygdeavgift module) for trygdeavgift.
> They are never stored on the medlemskapsperiode.

## Valid Trygdedekninger by Behandlingstema

Defined in `GyldigeTrygdedekningerService`, then intersected with the bestemmelse via
`LovligeKombinasjonerTrygdedekningBestemmelse`:

| Behandlingstema | Available Trygdedekninger |
|-----------------|---------------------------|
| YRKESAKTIV | `FULL_DEKNING_FTRL`, the full `FTRL_2_9_*` family, `FTRL_2_7_TREDJE_LEDD_B_*`, `FTRL_2_7A_ANDRE_LEDD_B_*`, `TILLEGGSAVTALE_NATO_HELSEDEL` |
| IKKE_YRKESAKTIV | `FULL_DEKNING_FTRL`, the `FTRL_2_9_*` family, `FTRL_2_7_TREDJE_LEDD_B_*`, `TILLEGGSAVTALE_NATO_HELSEDEL` |
| PENSJONIST | `FULL_DEKNING_FTRL`, `FTRL_2_9_*` (helse/pensjon variants), `FTRL_2_7_TREDJE_LEDD_B_*` |

**Note**: NATO host-country dekning (`TILLEGGSAVTALE_NATO_HELSEDEL`) gives helsedel only.

## InnvilgelsesResultat

### INNVILGET
- Membership granted
- Period active and valid
- Synced to MEDL with status GYLD

### DELVIS_INNVILGET
- Partially granted (e.g. only part of the requested period innvilget)

### AVSLAATT
- Membership denied
- Created when søknad partially rejected
- Example: Period before mottaksdato

### OPPHØRT
- Membership terminated
- Previously INNVILGET period ended
- Synced to MEDL with status AVST

## Type Determination Logic

```kotlin
// From UtledMedlemskapstype.kt
fun av(bestemmelse: Bestemmelse): Medlemskapstyper {
    // Special case: §2-15 andre ledd is always FRIVILLIG
    if (bestemmelse === Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD) {
        return Medlemskapstyper.FRIVILLIG
    }

    // Check if bestemmelse is in PLIKTIG list
    if (bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser) {
        return Medlemskapstyper.PLIKTIG
    }

    // Default: FRIVILLIG
    return Medlemskapstyper.FRIVILLIG
}
```

## Implications of Type

### PLIKTIG Membership
- No trygdeavgift via NAV (employer handles via skattetrekk)
- Automatic renewal while conditions met
- Cannot voluntarily terminate

### FRIVILLIG Membership
- Trygdeavgift invoiced by NAV (forskuddsfakturering)
- 25% rule may cap avgift amount
- Can be terminated by member
- Requires active payment to maintain
