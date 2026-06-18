# AvklarteFakta Reference

## Overview

Avklarte fakta (clarified facts) are answers the saksbehandler must give before certain vilkår can be presented for a bestemmelse — e.g. what the arbeidssituasjon is, or which familierelasjon applies. They are NOT a fetch of raw facts from PDL/Aareg; they are a small set of choices derived from søknadsland and relasjon, which then steer which vilkår `VilkårForBestemmelse*` returns.

## AvklarteFaktaForBestemmelse

A single `@Component` (no injected per-tema subcomponents). It routes by behandlingstema to internal `hent*` methods and returns `List<AvklarteFaktaType>`:

```kotlin
@Component
class AvklarteFaktaForBestemmelse(
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val behandlingService: BehandlingService
) {
    fun hentAvklarteFakta(bestemmelse: Bestemmelse, behandlingID: Long): List<AvklarteFaktaType> {
        val behandlingstema = behandlingService.hentBehandling(behandlingID).tema
        return when (behandlingstema) {
            Behandlingstema.IKKE_YRKESAKTIV -> hentAvklarteFaktaIkkeYrkesaktiv(bestemmelse, behandlingID)
            Behandlingstema.PENSJONIST      -> hentAvklarteFaktaPensjonist(bestemmelse, behandlingID)
            else                            -> hentAvklarteFaktaYrkesaktiv(bestemmelse, behandlingID)
        }
    }
    // hentAvklarteFaktaIkkeYrkesaktiv / hentAvklarteFaktaPensjonist / hentAvklarteFaktaYrkesaktiv ...
}

data class AvklarteFaktaType(val type: Avklartefaktatyper, val muligeFakta: List<String>)
```

## Avklartefaktatyper used for FTRL kap. 2

`Avklartefaktatyper` is a generated kodeverk enum (`no.nav.melosys.domain.kodeverk.Avklartefaktatyper`). The values actually produced by `AvklarteFaktaForBestemmelse`:

| Avklartefaktatyper | muligeFakta (enum names) | Source of the branch |
|--------------------|--------------------------|----------------------|
| `ARBEIDSSITUASJON` | `Arbeidssituasjontype` values (e.g. `ARBIED_I_NORGE_2_2`, `ARBEID_PÅ_NORSK_SOKKEL_2_2`, `MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD`, `VEKSELVIS_ARBEID_2_1_FJERDE_LEDD`) | YRKESAKTIV; søknadsland for 2-1 |
| `IKKE_YRKESAKTIV_RELASJON` | `Ikkeyrkesaktivrelasjontype` values (e.g. `BARN_2_5_ANDRE_LEDD`, `EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B`, `EKTEFELLE_2_8_FJERDE_LEDD`) | IKKE_YRKESAKTIV / PENSJONIST 2-5 / 2-8 |
| `IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD` | `Ikkeyrkesaktivoppholdtype` values (`MIDLERTIDIG_2_1_FJERDE_LEDD`, `VEKSELVIS_2_1_FJERDE_LEDD`) | IKKE_YRKESAKTIV 2-1 with utlandsopphold |

## Which bestemmelser require avklarte fakta

Only a subset of bestemmelser produce avklarte fakta; the rest return an empty list (no clarification needed before showing vilkår):

- **YRKESAKTIV**: `FTRL_KAP2_2_1` (søknadsland → arbeidssituasjon) and `FTRL_KAP2_2_2` (arbeidssituasjon Norge/sokkel).
- **IKKE_YRKESAKTIV**: `FTRL_KAP2_2_1` (søknadsland → opphold), `FTRL_KAP2_2_5_ANDRE_LEDD` (relasjon barn/ektefelle) and `FTRL_KAP2_2_8_FJERDE_LEDD` (relasjon barn/ektefelle).
- **PENSJONIST**: `FTRL_KAP2_2_1` and `FTRL_KAP2_2_5_ANDRE_LEDD` (ektefelle-relasjon only).

## Fact Collection Flow

```
1. Bestemmelse + behandling selected
           │
           ▼
2. AvklarteFaktaForBestemmelse.hentAvklarteFakta(bestemmelse, behandlingID)
           │  (reads behandlingstema + soeknadsland via MottatteOpplysningerService)
           ▼
3. UI presents the avklarte fakta choices (type + muligeFakta)
           │
           ▼
4. Saksbehandler picks a value → sent back as avklarteFakta: Map<Avklartefaktatyper, String>
           │
           ▼
5. VilkårForBestemmelse.hentVilkår(..., avklarteFakta, behandlingID) returns the matching vilkår
```

## How avklarte fakta feed back into vilkår

The chosen value is passed back into `VilkårForBestemmelse.hentVilkår(...)` as `avklarteFakta: Map<Avklartefaktatyper, String>`. The theme routers read it via:

- `avklarteFakta[Avklartefaktatyper.ARBEIDSSITUASJON]` → `Arbeidssituasjontype.valueOf(...)` (yrkesaktiv 2-1 / 2-2)
- `avklarteFakta[Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON]` → `Ikkeyrkesaktivrelasjontype.valueOf(...)` (2-5 andre ledd, 2-8 fjerde ledd)

An unknown/missing value throws `FunksjonellException`.

## Storage

Avklarte fakta are a derivation step in the request/response cycle, driven by `soeknadsland` (from `MottatteOpplysningerService`) and the saksbehandler's relasjon choice. They are not persisted as their own entity. The resulting evaluation (which begrunnelse-koder were chosen) is stored as `VilkaarBegrunnelse` rows on the `Vilkaarsresultat`.
