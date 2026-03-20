# Membership Types Reference

## Medlemskapstyper

### PLIKTIG (Mandatory Membership)

Persons who are **required** to be members of folketrygden based on residence or employment in Norway.

**Determining Bestemmelser** (from `PliktigeMedlemskapsbestemmelser`):

| Bestemmelse | Description | FTRL Reference |
|-------------|-------------|----------------|
| `FTRL_KAP2_2_1` | Bosatt i Norge | §2-1 |
| `FTRL_KAP2_2_2` | Arbeidstaker i Norge | §2-2 |
| `FTRL_KAP2_2_3_ANDRE_LEDD` | Sokkelarbeidere | §2-3 andre ledd |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_A` | Statens tjenesteperson | §2-5 første ledd a |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_B` | Tjenesteperson ved utenrikstjenesten | §2-5 første ledd b |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_C` | Person engasjert av utenrikstjenesten | §2-5 første ledd c |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_D` | Arbeidstaker i hotell- og restaurantnæring | §2-5 første ledd d |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_E` | Ansatt i internasjonal organisasjon | §2-5 første ledd e |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_F` | Misjonær/person i religiøs organisasjon | §2-5 første ledd f |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_G` | Au pair utenfor EØS | §2-5 første ledd g |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_H` | Student i utlandet | §2-5 første ledd h |
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

### FULL_DEKNING
Complete coverage including:
- Helsedel (health benefits)
- Sykepenger (sick pay)
- Pensjonsdel (pension accrual)
- Yrkesskadetrygd (occupational injury insurance)

**Available for**: YRKESAKTIV, IKKE_YRKESAKTIV

### HELSEDEL_MED_SYKEPENGER
Health coverage including sick pay:
- Helseutgifter dekket
- Sykepenger ved arbeidsuførhet
- No pension accrual

**Available for**: YRKESAKTIV, IKKE_YRKESAKTIV

### HELSEDEL_UTEN_SYKEPENGER
Health coverage without sick pay:
- Helseutgifter dekket
- No sykepenger
- No pension accrual

**Available for**: YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST

### PENSJONSDEL_MED_YRKESSKADETRYGD
Pension coverage with occupational injury:
- Pensjonsopptjening
- Yrkesskadedekning
- No health benefits

**Available for**: Special cases

### PENSJONSDEL_UTEN_YRKESSKADETRYGD
Pension coverage only:
- Pensjonsopptjening only
- No yrkesskadedekning
- No health benefits

**Available for**: Special cases

## Coverage Matrix by Behandlingstema

| Behandlingstema | FULL | HELSE_MED_SYKE | HELSE_UTEN_SYKE | PENSJON_MED | PENSJON_UTEN |
|-----------------|------|----------------|-----------------|-------------|--------------|
| YRKESAKTIV | ✓ | ✓ | ✓ | - | - |
| IKKE_YRKESAKTIV | ✓ | ✓ | ✓ | - | - |
| PENSJONIST | - | - | ✓ | - | - |

**Note**: Pensjonister can only get helsedel coverage (§2-7, §2-7a).

## InnvilgelsesResultat

### INNVILGET
- Membership granted
- Period active and valid
- Synced to MEDL with status GYLD

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
