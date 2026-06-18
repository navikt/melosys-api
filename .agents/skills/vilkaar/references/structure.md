# Vilkår Structure Reference

## Architecture Overview

```
VilkårForBestemmelse (router)
        │
        ├── VilkårForBestemmelseYrkesaktiv
        │       └── Returns vilkår for YRKESAKTIV behandlingstema
        │
        ├── VilkårForBestemmelseIkkeYrkesaktiv
        │       └── Returns vilkår for IKKE_YRKESAKTIV behandlingstema
        │
        └── VilkårForBestemmelsePensjonist
                └── Returns vilkår for PENSJONIST behandlingstema
```

## VilkårForBestemmelse Router

```kotlin
@Component
class VilkårForBestemmelse(
    val vilkårForBestemmelseYrkesaktiv: VilkårForBestemmelseYrkesaktiv,
    val vilkårForBestemmelseIkkeYrkesaktiv: VilkårForBestemmelseIkkeYrkesaktiv,
    val vilkårForBestemmelsePensjonist: VilkårForBestemmelsePensjonist
) {
    fun hentVilkår(
        bestemmelse: Bestemmelse,
        behandlingstema: Behandlingstema,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> = when (behandlingstema) {
        Behandlingstema.PENSJONIST -> vilkårForBestemmelsePensjonist.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
        Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
        Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktiv.hentVilkår(bestemmelse, avklarteFakta, behandlingID)
        else -> emptyList()
    }
}
```

The theme classes take `hentVilkår(bestemmelse, avklarteFakta, behandlingID)`. Land-driven branching is **søknadsland-driven** inside the theme classes via `MottatteOpplysningerService.hentMottatteOpplysninger(behandlingID).mottatteOpplysningerData?.soeknadsland`, not a `statsborgerskap` parameter.

## Vilkår Data Class

```kotlin
data class Vilkår(
    val vilkår: Vilkaar,                              // The enum value
    val muligeBegrunnelser: Collection<String> = emptyList(), // Predefined reasons
    val defaultOppfylt: Boolean? = null               // Default evaluation value
)
```

### muligeBegrunnelser
Pre-defined reason codes (`kode`) a saksbehandler can select. Loaded from begrunnelse-kodeverk via `toStringList(...)`, e.g. `Ftrl_2_7_begrunnelser` for `FTRL_2_7_RIMELIGHETSVURDERING` and `Ftrl_2_8_naer_tilknytning_norge_begrunnelser` for `FTRL_2_8_NÆR_TILKNYTNING_NORGE`.

### defaultOppfylt
When set:
- `true`: Vilkår is pre-marked as fulfilled (e.g. `FTRL_2_5_MEDFØLGENDE_A_E` for ektefelle-relasjon)
- `null`: Saksbehandler must evaluate

## Vilkår per Bestemmelse

The vilkår-set is keyed on the `Bestemmelse` enum (e.g. `FTRL_KAP2_2_5_FØRSTE_LEDD_A`), dispatched by behandlingstema. Below are the actual `when(bestemmelse)` mappings from the three router classes (`VilkårForBestemmelse{Yrkesaktiv,IkkeYrkesaktiv,Pensjonist}.kt`).

### YRKESAKTIV (`VilkårForBestemmelseYrkesaktiv`)

| Bestemmelse | Vilkår |
|-------------|--------|
| `FTRL_KAP2_2_1` | branches on søknadsland + arbeidssituasjon (bosatt-Norge: `FTRL_2_1_BOSATT_NORGE`, `FTRL_2_11_UNNTAK_AMBASSADEPERSONELL_MELLOMFOLKELIG_ORG`, `FTRL_2_1_LOVLIG_OPPHOLD`; midlertidig/vekselvis: `FTRL_2_1_*_UNDER_12MND`, `FTRL_2_14_*`, `FTRL_2_1_LOVLIG_OPPHOLD`) |
| `FTRL_KAP2_2_2` | branches on arbeidssituasjon (Norge / sokkel): `FTRL_ARBEIDSTAKER`, `FTRL_2_2_LOVLIG_ADGANG_ARBEID` + (`FTRL_2_11_*` eller `FTRL_2_2_INNRETNING_NATURRESSURSER`) |
| `FTRL_KAP2_2_3_ANDRE_LEDD` | `FTRL_ARBEIDSTAKER`, `FTRL_2_3_ARBEIDSGIVER_SVALBARD_JAN_MAYEN` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_A` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_ARBEIDSTAKER`, `FTRL_2_5_NORSKE_STATS_TJENESTE` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_B` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_ARBEIDSTAKER`, `FTRL_2_5_ARBEID_FOR_PERSON_I_NORSKE_STATS_TJENESTE` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_C` | `FTRL_2_5_I_FORSVARETS_TJENESTE` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_D` | `FTRL_2_5_FREDSKORPSDELTAKER_EKSPERT_UTVIKLINGSLAND` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_E` | `FTRL_2_5_NATO_SIVILE_KRIGSTIDSORGANGER` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_F` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_ARBEIDSTAKER`, `FTRL_2_5_NORSK_SKIP`, `FTRL_2_12_UNNTAK_TURISTSKIP` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_G` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_ARBEIDSTAKER`, `FTRL_2_5_NORSK_SIVILT_LUFTFARTSSELSKAP` |
| `FTRL_KAP2_2_7_FØRSTE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_IKKE_PLIKTIG_MEDLEM`, `FTRL_2_7_RIMELIGHETSVURDERING` |
| `FTRL_KAP2_2_7A` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7A_BOSATT_I_NORGE`, `FTRL_2_7A_SKIP_UTENFOR_EØS` |
| `FTRL_KAP2_2_8_FØRSTE_LEDD_A` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_FØRSTE_LEDD_B` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_STUDENT_UVIVERSITET_HØGSKOLE`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_ANDRE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |

(Plus vertslandsavtale-bestemmelser: `ARKTISK_RÅDS_SEKRETARIAT_ART16`, `DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14`, `DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16`, `TILLEGGSAVTALE_NATO`.)

### IKKE_YRKESAKTIV (`VilkårForBestemmelseIkkeYrkesaktiv`)

| Bestemmelse | Vilkår |
|-------------|--------|
| `FTRL_KAP2_2_1` | søknadsland-branched bosatt-vilkår + `FTRL_2_1_LOVLIG_OPPHOLD` |
| `FTRL_KAP2_2_5_FØRSTE_LEDD_H` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_2_5_LÅN_STIPEND_LÅNEKASSEN` |
| `FTRL_KAP2_2_5_ANDRE_LEDD` | branches on `IKKE_YRKESAKTIV_RELASJON` (barn / ektefelle): `FTRL_2_5_MEDFØLGENDE_A_E`, `FTRL_2_5_FORSØRGET_FAMILIEMEDLEM` (+ `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER` or `FTRL_FORUTGÅENDE_TRYGDETID`) |
| `FTRL_KAP2_2_7_FØRSTE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_IKKE_PLIKTIG_MEDLEM`, `FTRL_2_7_RIMELIGHETSVURDERING` |
| `FTRL_KAP2_2_7_FJERDE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_FORSØRGET_FAMILIEMEDLEM`, `FTRL_2_7_INGEN_SÆRLIGE_GRUNNER_TALER_IMOT` |
| `FTRL_KAP2_2_8_FØRSTE_LEDD_B` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_STUDENT_UVIVERSITET_HØGSKOLE`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_FØRSTE_LEDD_C`, `FTRL_KAP2_2_8_ANDRE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_FJERDE_LEDD` | branches on `IKKE_YRKESAKTIV_RELASJON` (barn / ektefelle): `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_FORSØRGET_FAMILIEMEDLEM` (+ `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE`) |

### PENSJONIST (`VilkårForBestemmelsePensjonist`)

| Bestemmelse | Vilkår |
|-------------|--------|
| `FTRL_KAP2_2_1` | søknadsland-branched bosatt-vilkår + `FTRL_2_1_LOVLIG_OPPHOLD` |
| `FTRL_KAP2_2_5_ANDRE_LEDD` | branches on ektefelle-`IKKE_YRKESAKTIV_RELASJON` |
| `FTRL_KAP2_2_7_FØRSTE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_IKKE_PLIKTIG_MEDLEM`, `FTRL_2_7_RIMELIGHETSVURDERING` |
| `FTRL_KAP2_2_7_FJERDE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_FORSØRGET_FAMILIEMEDLEM`, `FTRL_2_7_INGEN_SÆRLIGE_GRUNNER_TALER_IMOT` |
| `FTRL_KAP2_2_8_FØRSTE_LEDD_D` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_PENSJON_UFØRETRYGD_FOLKETRYGDEN`, `FTRL_2_8_PENSJONIST_TRETTI_ÅR_TRYGDETID`, `FTRL_2_8_PENSJONIST_TI_ÅR_TRYGDETID_FØR_SØKNADSTIDSPUNKT`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_ANDRE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |
| `FTRL_KAP2_2_8_FJERDE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_FORSØRGET_FAMILIEMEDLEM`, `FTRL_FORUTGÅENDE_TRYGDETID`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |

The pensjonist provision is § 2-8 første ledd **bokstav d** (`FTRL_KAP2_2_8_FØRSTE_LEDD_D`), gated on 30 years' trygdetid after age 16 and 10 years immediately before søknadstidspunkt.

## Vilkaarsresultat Entity

```java
@Entity
@Table(name = "vilkaarsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Vilkaarsresultat extends RegistreringsInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkaar")
    private Vilkaar vilkaar;

    @Column(name = "oppfylt")
    private boolean oppfylt;

    @OneToMany(mappedBy = "vilkaarsresultat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VilkaarBegrunnelse> begrunnelser = new HashSet<>();

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    @Column(name = "begrunnelse_fritekst_eessi")
    private String begrunnelseFritekstEessi;
}
```

`VilkaarBegrunnelse` (table `vilkaar_begrunnelse`) holds one row per begrunnelse-kode:

```java
@Entity
@Table(name = "vilkaar_begrunnelse")
public class VilkaarBegrunnelse extends RegistreringsInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vilkaar_resultat_id", nullable = false, updatable = false)
    private Vilkaarsresultat vilkaarsresultat;

    @Column(name = "kode")
    private String kode;
}
```

## Database Schema

Source of truth: `app/.../db/migration/melosysDB/V1.0_09__VILKAARSRESULTAT.sql`.

```sql
CREATE TABLE vilkaarsresultat (
    id                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    beh_resultat_id       NUMBER(19) NOT NULL,   -- FK -> behandlingsresultat.behandling_id
    vilkaar               VARCHAR2(99) NOT NULL,
    oppfylt               NUMBER(1) NOT NULL,
    begrunnelse_fritekst  VARCHAR2(4000) NULL,
    -- + RegistreringsInfo audit columns (registrert_*, endret_*)
    CONSTRAINT pk_vilkaarsresultat PRIMARY KEY (id)
);

CREATE TABLE vilkaar_begrunnelse (
    id                    NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    vilkaar_resultat_id   NUMBER(19) NOT NULL,   -- FK -> vilkaarsresultat.id
    kode                  VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_vilkaar_begrunnelse PRIMARY KEY (id)
);
```

(`begrunnelse_fritekst_eessi` is added by a later migration, `V111__oppdater_vilkaarsresultat_med_nye_koder.sql`.)

## Adding New Vilkår

1. Add the enum value to the `Vilkaar` kodeverk (generated `no.nav.melosys.domain.kodeverk.Vilkaar`)
2. Add it to the appropriate `VilkårForBestemmelse*` class under the relevant `Bestemmelse`
3. Create a Flyway migration if any new database columns are needed
4. Update frontend to display the new vilkår
