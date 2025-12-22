# MEDL Data Mapping

How domain entities map to MEDL API structures.

## MedlPeriodeKonverter

Central converter class for domain → MEDL mapping.

**Location**: `integrasjon/src/main/kotlin/no/nav/melosys/integrasjon/medl/MedlPeriodeKonverter.kt`

## Lovvalgsbestemmelse → GrunnlagMedl

### EU Regulation 883/2004

| Lovvalgsbestemmelse | GrunnlagMedl |
|---------------------|--------------|
| `ART_11_3_A` | `FO_11_3_A` |
| `ART_11_3_B` | `FO_11_3_B` |
| `ART_11_3_C` | `FO_11_3_C` |
| `ART_11_3_D` | `FO_11_3_D` |
| `ART_11_3_E` | `FO_11_3_E` |
| `ART_11_4` | `FO_11_4` |
| `ART_12_1` | `FO_12_1` |
| `ART_12_2` | `FO_12_2` |
| `ART_13_1_A` | `FO_13_1_A` |
| `ART_13_1_B_I` | `FO_13_1_B_1` |
| `ART_13_1_B_II` | `FO_13_1_B_2` |
| `ART_13_1_B_III` | `FO_13_1_B_3` |
| `ART_13_1_B_IV` | `FO_13_1_B_4` |
| `ART_13_2_A` | `FO_13_2_A` |
| `ART_13_2_B` | `FO_13_2_B` |
| `ART_13_3` | `FO_13_3` |
| `ART_13_4` | `FO_13_4` |
| `ART_16_1` | `FO_16_1` |

### Article 13 with Tilleggsbestemmelse

When Article 13 has a tilleggsbestemmelse, it maps to the tilleggsbestemmelse instead:

```kotlin
// Art. 13 + Art. 11.4.1 → FO_11_4_1
if (bestemmelse == ART_13_* && tilleggsbestemmelse == ART_11_4_1) {
    return GrunnlagMedl.FO_11_4_1
}

// Art. 13 (EFTA) + Art. 13.4.1 → KONV_STORBRIT_NIRLAND_13_4_1
if (bestemmelse == ART_13_EFTA_* && tilleggsbestemmelse == ART_13_4_1) {
    return GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_4_1
}
```

### Transition Rules (Overgangsregler)

For Art. 87.8 and Art. 87a, uses overgangsregelbestemmelser:

```kotlin
when (overgangsregelbestemmelse) {
    ART_14_1_A -> FO_1408_14_1_A
    ART_14_1_B -> FO_1408_14_1_B
    ART_14_2_A_I -> FO_1408_14_2_A_1
    // ... etc
}
```

## Trygdedekninger → DekningMedl

### EU/EEA Cases (tilMedlTrygdeDekning)

| Trygdedekninger | DekningMedl |
|-----------------|-------------|
| `FULL_DEKNING` | `FULL` |
| `FULL_DEKNING_FOLKETRYGDLOVEN` | `FULL` |
| `FULL_DEKNING_FRIVILLIG` | `FULL` |
| `UTEN_DEKNING` | `UNNTATT` |
| `MED_RETT_TIL_DEKNING` | `FULL` |

### FTRL Cases (tilMedlTrygdedekningForFtrl)

More specific mapping for Norwegian law:

| Trygdedekninger | DekningMedl |
|-----------------|-------------|
| `FULL_DEKNING_FOLKETRYGDLOVEN` | `FULL` |
| `UTEN_PENSJONSDEL_FTRL_2_7_3_B` | `FTRL_2_7_3_LEDD_B` |
| `UTEN_PENSJONSDEL_FTRL_2_7_2` | `FTRL_2_7_2_LEDD` |
| `FOLKETRYGDLOVEN_2_14` | `IKKEMED_FTRL_2_14` |
| `NATO_AVTALEN_FRADRAG` | `FRAVTALE_NATO_FRADRAG` |

## Land → ISO3 Country Code

Uses `IsoLandkodeKonverterer`:

| Land_iso2 | ISO3 |
|-----------|------|
| `NO` | `NOR` |
| `SE` | `SWE` |
| `DK` | `DNK` |
| `FI` | `FIN` |
| `DE` | `DEU` |
| `GB` | `GBR` |
| `US` | `USA` |

## Medlemskapstyper → MEDL Fields

| Medlemskapstyper | medlem | Notes |
|------------------|--------|-------|
| `PLIKTIG` | true | Mandatory membership |
| `FRIVILLIG` | true | Voluntary membership |
| `UNNTATT` | false | Exempt from membership |

## InnvilgelsesResultat → PeriodestatusMedl

| InnvilgelsesResultat | PeriodestatusMedl |
|----------------------|-------------------|
| `INNVILGET` | `GYLD` |
| `AVSLAATT` | `AVST` |
| `OPPHØRT` | `AVST` |

## DTO Construction Examples

### Create Lovvalgsperiode in MEDL

```kotlin
val dto = MedlemskapsunntakForPost(
    ident = behandling.fagsak.hentBrukersAktørID(),
    fraOgMed = lovvalgsperiode.fom,
    tilOgMed = lovvalgsperiode.tom,
    status = PeriodestatusMedl.GYLD.name,
    statusaarsak = null,
    dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(lovvalgsperiode.dekning).name,
    lovvalgsland = IsoLandkodeKonverterer.tilIso3(lovvalgsperiode.lovvalgsland),
    lovvalg = LovvalgMedl.ENDL.name,
    grunnlag = MedlPeriodeKonverter.tilGrunnlag(lovvalgsperiode.bestemmelse).name,
    sporingsinformasjon = SporingsinformasjonForPost(
        kildedokument = hentKildedokumenttype(behandling).name
    )
)
```

### Update Existing Period

```kotlin
// First fetch existing to get version
val existing = medlService.hentEksisterendePeriode(medlPeriodeID)

val dto = MedlemskapsunntakForPut(
    unntakId = medlPeriodeID,
    fraOgMed = periode.fom,
    tilOgMed = periode.tom,
    // ... other fields
    sporingsinformasjon = SporingsinformasjonForPut(
        kildedokument = "...",
        versjon = existing.sporingsinformasjon.versjon  // Critical!
    )
)
```

## Unsupported Mappings

These throw `TekniskException`:

```kotlin
// Unsupported dekning
throw TekniskException("Dekningstype støttes ikke: $dekning")

// Unsupported bestemmelse
throw TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL: $bestemmelse")

// Missing overgangsregler
throw FunksjonellException("Grunnlaget $grunnlag og overgangsregler skal benyttes, men er tom")
