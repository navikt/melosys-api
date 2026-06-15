# MEDL Data Mapping

How domain entities map to MEDL API structures.

## MedlPeriodeKonverter

Central converter class for domain → MEDL mapping.

**Location**: `integrasjon/src/main/kotlin/no/nav/melosys/integrasjon/medl/MedlPeriodeKonverter.kt`

## Lovvalgsbestemmelse → GrunnlagMedl

### EU Regulation 883/2004

Source: the `lovvalgsbestemmelseTilGrunnlagMedlTabell` map in `MedlPeriodeKonverter.kt`.
Left column is the `Lovvalgbestemmelser_883_2004` enum constant; right column is the `GrunnlagMedl` constant.

| Lovvalgbestemmelse | GrunnlagMedl |
|--------------------|--------------|
| `FO_883_2004_ART11_3A` | `FO_11_3_A` |
| `FO_883_2004_ART11_3B` | `FO_11_3_B` |
| `FO_883_2004_ART11_3C` | `FO_11_3_C` |
| `FO_883_2004_ART11_3D` | `FO_11_3_D` |
| `FO_883_2004_ART11_3E` | `FO_11_3_E` |
| `FO_883_2004_ART11_4` | `FO_11_4` |
| `FO_883_2004_ART12_1` | `FO_12_1` |
| `FO_883_2004_ART12_2` | `FO_12_2` |
| `FO_883_2004_ART13_1A` | `FO_13_1_A` |
| `FO_883_2004_ART13_1B1` | `FO_13_1_B` |
| `FO_883_2004_ART13_1B2` | `FO_13_B_II` |
| `FO_883_2004_ART13_1B3` | `FO_13_B_III` |
| `FO_883_2004_ART13_1B4` | `FO_13_B_IV` |
| `FO_883_2004_ART13_2A` | `FO_13_2_A` |
| `FO_883_2004_ART13_2B` | `FO_13_2_B` |
| `FO_883_2004_ART13_3` | `FO_13_3` |
| `FO_883_2004_ART13_4` | `FO_13_4` |
| `FO_883_2004_ART15` | `FO_15` |
| `FO_883_2004_ART16_1` (and `ART16_2`) | `FO_16` |

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

For Art. 87.8 and Art. 87a, `MedlService` calls
`MedlPeriodeKonverter.tilGrunnlagMedltypeFraOvergangsregler(overgangsregelbestemmelser[0])`.
The `Overgangsregelbestemmelser` → `GrunnlagMedl` entries live in the same
`lovvalgsbestemmelseTilGrunnlagMedlTabell`:

```kotlin
Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A -> GrunnlagMedl.FO_1408_14_2_A
Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B -> GrunnlagMedl.FO_1408_14_2_B
Overgangsregelbestemmelser.FO_1408_1971_ART14A_2  -> GrunnlagMedl.FO_1408_14_A_2
Overgangsregelbestemmelser.FO_1408_1971_ART14C_A  -> GrunnlagMedl.FO_1408_14_C_A
Overgangsregelbestemmelser.FO_1408_1971_ART14C_B  -> GrunnlagMedl.FO_1408_14_C_B
```

## Trygdedekninger → DekningMedl

### EU/EEA Cases (`tilMedlTrygdeDekning`)

| Trygdedekninger | DekningMedl |
|-----------------|-------------|
| `FULL_DEKNING_EOSFO`, `FULL_DEKNING_FTRL`, `FULL_DEKNING` | `FULL` |
| `UTEN_DEKNING` | `UNNTATT` |
| `UNNTATT_CAN_7_5_B`, `UNNTATT_USA_5_2_G` | `IKKE_PENSJONSDEL` |

Anything else throws `TekniskException("Dekningstype støttes ikke: ...")`.

### FTRL Cases (`tilMedlTrygdedekningForFtrl`)

More specific mapping for Norwegian law (subset — see `MedlPeriodeKonverter.kt`):

| Trygdedekninger | DekningMedl |
|-----------------|-------------|
| `FULL_DEKNING_FTRL` | `FULL` |
| `FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER` | `FTRL_2_7_3_LEDD_B` |
| `FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER` | `FTRL_2_7A_2_LEDD_B` |
| `FTRL_2_9_FØRSTE_LEDD_A_HELSE` … (several §2-9 variants) | `FTRL_2_9_1_LEDD_A` … |
| `TILLEGGSAVTALE_NATO_HELSEDEL` | `TILLEGSAVTALE_NATO_DEKNING` |

Anything else throws `TekniskException("Dekningstype støttes ikke for FTRL: ...")`.

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

`MedlService.opprettPeriode` builds the request in two stages — the constructor sets
`fraOgMed`/`tilOgMed`/`dekning`/`lovvalgsland`/`grunnlag`, then `.apply{}` sets
`sporingsinformasjon`/`ident`/`lovvalg`/`status`/`statusaarsak`. All wire values use `.kode`:

```kotlin
val grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
    MedlPeriodeKonverter.hentLovvalgBestemmelse(periodeOmLovvalg)
).kode

MedlemskapsunntakForPost(
    fraOgMed = periodeOmLovvalg.fom,
    tilOgMed = periodeOmLovvalg.tom,
    dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(periodeOmLovvalg.dekning).kode,
    lovvalgsland = IsoLandkodeKonverterer.tilIso3(periodeOmLovvalg.lovvalgsland.kode),
    grunnlag = grunnlag
).apply {
    ident = fnr
    lovvalg = LovvalgMedl.ENDL.kode
    status = PeriodestatusMedl.GYLD.kode
    statusaarsak = null
    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
        kildedokument = kildedokumenttypeMedl.kode
    )
}
```

### Update Existing Period

`MedlService.lovvalgRequestForPut` fetches the existing period (via the private helper
`hentEksisterendePeriode`, which delegates to `MedlemskapClient.hentPeriode`) to read its version:

```kotlin
val eksisterendePeriode = hentEksisterendePeriode(medlPeriodeID)  // private in MedlService

MedlemskapsunntakForPut(
    unntakId = medlPeriodeID,
    fraOgMed = periodeOmLovvalg.fom,
    tilOgMed = periodeOmLovvalg.tom,
    dekning = MedlPeriodeKonverter.tilMedlTrygdeDekning(periodeOmLovvalg.dekning).kode,
    lovvalgsland = IsoLandkodeKonverterer.tilIso3(periodeOmLovvalg.lovvalgsland.kode),
    grunnlag = MedlPeriodeKonverter.tilGrunnlagMedltype(
        MedlPeriodeKonverter.hentLovvalgBestemmelse(periodeOmLovvalg)
    ).kode,
    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
        kildedokument = kildedokumenttypeMedl.kode,
        versjon = eksisterendePeriode.sporingsinformasjon!!.versjon  // Critical!
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
