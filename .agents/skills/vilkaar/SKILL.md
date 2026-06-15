---
name: vilkaar
description: |
  Expert knowledge of vilkår / vilkårsvurdering (requirements evaluation) in melosys-api.
  Use when: (1) Understanding inngangsvilkår structure for FTRL bestemmelser (VilkårForBestemmelse),
  (2) Debugging Vilkaarsresultat / oppfylt status or begrunnelser,
  (3) Understanding avklarte fakta (AvklarteFaktaForBestemmelse) collection,
  (4) Investigating why a vilkår fails or which vilkår a bestemmelse requires.
  Triggers: vilkår, vilkaar, vilkaarsresultat, vilkårsvurdering, oppfylt, begrunnelse,
  inngangsvilkår, avklarte fakta, FTRL_2_5, FTRL_2_8, VilkårForBestemmelse.
---

# Vilkaar Skill

## Quick Reference

### What is Vilkår?

Vilkår are requirements/conditions that must be evaluated to determine if a person qualifies for a specific FTRL bestemmelse (legal provision). Each bestemmelse has its own set of vilkår that must be fulfilled (oppfylt).

### Domain Model

```
Behandling
    └── Behandlingsresultat
            └── Vilkaarsresultat (per vilkår)
                    ├── vilkaar: Vilkaar (enum)
                    ├── oppfylt: boolean
                    ├── begrunnelser: Set<VilkaarBegrunnelse>  (each has a kode)
                    ├── begrunnelseFritekst: String?
                    └── begrunnelseFritekstEessi: String?
```

### Key Components

| Component | Description | Location |
|-----------|-------------|----------|
| `VilkårForBestemmelse` | Routes to theme-specific vilkår | service/.../ftrl/bestemmelse/vilkaar/ |
| `Vilkår` | Data class holding vilkår definition (`vilkår: Vilkaar`, `muligeBegrunnelser`, `defaultOppfylt`) | service/.../ftrl/bestemmelse/vilkaar/Vilkår.kt |
| `Vilkaarsresultat` | Entity storing evaluation result | domain/.../Vilkaarsresultat.java |
| `VilkaarBegrunnelse` | Entity holding a begrunnelse-kode per resultat | domain/.../VilkaarBegrunnelse.java |
| `AvklarteFaktaForBestemmelse` | Determines required facts (avklarte fakta) per bestemmelse | service/.../ftrl/bestemmelse/avklartefakta/ |
| `VilkaarsresultatService` | Reads/saves vilkårsresultater | service/.../behandling/VilkaarsresultatService.kt |
| `VilkaarController` | REST entry point `/vilkaar/{behandlingID}` (GET hent, POST registrer) | frontend-api/.../tjenester/gui/VilkaarController.java |

### Vilkår enum (Vilkaar)

The `Vilkaar` enum lives in the generated kodeverk (`no.nav.melosys.domain.kodeverk.Vilkaar`). FTRL-bestemmelse vilkår are `FTRL_`-prefixed; EØS/konvensjon utsendings- og unntaksvilkår use `FO_883_2004_*` / `KONV_EFTA_STORBRITANNIA_*`. Examples actually used in the bestemmelse-routers:

```kotlin
// FTRL kap. 2 vilkår (selection)
FTRL_ARBEIDSTAKER
FTRL_2_1_BOSATT_NORGE, FTRL_2_1_BOSATT_NORGE_FORUT, FTRL_2_1_LOVLIG_OPPHOLD
FTRL_2_1_OPPHOLD_UNDER_12MND, FTRL_2_1_ARBEID_OPPHOLD_UNDER_12MND
FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER, FTRL_2_5_MEDFØLGENDE_A_E, FTRL_2_5_FORSØRGET_FAMILIEMEDLEM
FTRL_2_1A_TRYGDEKOORDINGERING, FTRL_2_7_RIMELIGHETSVURDERING, FTRL_2_7_IKKE_PLIKTIG_MEDLEM
FTRL_FORUTGÅENDE_TRYGDETID, FTRL_2_8_NÆR_TILKNYTNING_NORGE
FTRL_2_8_PENSJONIST_TRETTI_ÅR_TRYGDETID, FTRL_2_8_PENSJONIST_TI_ÅR_TRYGDETID_FØR_SØKNADSTIDSPUNKT

// EØS/konvensjon (utsending/unntak), see VilkaarsresultatService
FO_883_2004_INNGANGSVILKAAR, FO_883_2004_ART12_1, FO_883_2004_ART12_2, FO_883_2004_ART16_1
KONV_EFTA_STORBRITANNIA_ART14_1, KONV_EFTA_STORBRITANNIA_ART16_1, KONV_EFTA_STORBRITANNIA_ART18_1
```

### Vilkår by Bestemmelse + Behandlingstema

The vilkår-set is keyed on the **`Bestemmelse`** enum (not a free-form behandlingstema list), and dispatched by behandlingstema via `VilkårForBestemmelse.hentVilkår(...)`. A few examples from the routers (see `references/structure.md` for the full mapping):

| Behandlingstema | Bestemmelse | Vilkår returned |
|-----------------|-------------|-----------------|
| YRKESAKTIV | `FTRL_KAP2_2_5_FØRSTE_LEDD_A` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_ARBEIDSTAKER`, `FTRL_2_5_NORSKE_STATS_TJENESTE` |
| YRKESAKTIV | `FTRL_KAP2_2_7_FØRSTE_LEDD` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_7_IKKE_PLIKTIG_MEDLEM`, `FTRL_2_7_RIMELIGHETSVURDERING` |
| IKKE_YRKESAKTIV | `FTRL_KAP2_2_5_FØRSTE_LEDD_H` | `FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER`, `FTRL_2_5_LÅN_STIPEND_LÅNEKASSEN` |
| PENSJONIST | `FTRL_KAP2_2_8_FØRSTE_LEDD_D` | `FTRL_2_1A_TRYGDEKOORDINGERING`, `FTRL_2_8_PENSJON_UFØRETRYGD_FOLKETRYGDEN`, `FTRL_2_8_PENSJONIST_TRETTI_ÅR_TRYGDETID`, `FTRL_2_8_PENSJONIST_TI_ÅR_TRYGDETID_FØR_SØKNADSTIDSPUNKT`, `FTRL_2_8_NÆR_TILKNYTNING_NORGE` |

Some bestemmelser (`FTRL_KAP2_2_1`, `FTRL_KAP2_2_2`, `2-5 andre ledd`, `2-8 fjerde ledd`) branch internally on **avklarte fakta** (arbeidssituasjon / familierelasjon) and søknadsland from `MottatteOpplysningerService`.

### Vilkår Evaluation Flow

```
1. Saksbehandler selects bestemmelse
           │
           ▼
2. VilkårForBestemmelse.hentVilkår(bestemmelse, behandlingstema, avklarteFakta, behandlingID)
           │
           ▼
3. System shows required vilkår in UI (List<Vilkår>)
           │
           ▼
4. Saksbehandler marks each vilkår as oppfylt/ikke oppfylt + begrunnelse
           │
           ▼
5. VilkaarController POST /vilkaar/{behandlingID}
   → VilkaarsresultatService.registrerVilkår(behandlingID, List<VilkaarDto>)
           │
           ▼
6. Vilkaarsresultat (+ VilkaarBegrunnelse) entities created on Behandlingsresultat
```

### AvklarteFakta (avklarte fakta)

Before some vilkår can be presented, the saksbehandler must clarify a fact (e.g. arbeidssituasjon, familierelasjon). `AvklarteFaktaForBestemmelse.hentAvklarteFakta(bestemmelse, behandlingID)` routes by behandlingstema and returns `List<AvklarteFaktaType>`, where:

```kotlin
data class AvklarteFaktaType(val type: Avklartefaktatyper, val muligeFakta: List<String>)
```

`Avklartefaktatyper` is a generated kodeverk enum. The values used for FTRL kap. 2 routing are `ARBEIDSSITUASJON` (muligeFakta = `Arbeidssituasjontype` names), `IKKE_YRKESAKTIV_RELASJON` (muligeFakta = `Ikkeyrkesaktivrelasjontype` names) and `IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD` (muligeFakta = `Ikkeyrkesaktivoppholdtype` names). The branch is driven by `soeknadsland` / relasjon from `MottatteOpplysningerService`, not by a fact-fetch from PDL/Aareg.

### Quick Debugging

Note: `vilkaarsresultat.beh_resultat_id` references `behandlingsresultat.behandling_id` (behandlingsresultat has a 1:1 PK = behandling_id). There is no `bestemmelse` column on behandlingsresultat — bestemmelse lives on `medlemskapsperiode`.

```sql
-- Check vilkår results for a behandling
SELECT vr.id, vr.vilkaar, vr.oppfylt, vr.begrunnelse_fritekst
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.beh_resultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId;

-- Vilkår with their begrunnelse-koder
SELECT vr.id, vr.vilkaar, vr.oppfylt, vb.kode
FROM vilkaarsresultat vr
LEFT JOIN vilkaar_begrunnelse vb ON vb.vilkaar_resultat_id = vr.id
WHERE vr.beh_resultat_id = :behandlingId;
```

## When to Use This Skill

- Understanding which vilkår apply to a bestemmelse
- Debugging why a bestemmelse cannot be selected
- Adding new vilkår to a bestemmelse
- Understanding avklarte fakta collection
- Investigating why vilkår are missing or have wrong oppfylt status

## Reference Files

- `references/structure.md` — full Vilkaar enum, per-bestemmelse vilkår mapping, entity + DB schema
- `references/avklartefakta.md` — AvklarteFaktaForBestemmelse routing and AvklarteFaktaType
- `references/evaluation.md` — save/read path via VilkaarController + VilkaarsresultatService
- `references/debugging.md` — SQL queries and code entry points

## Related Skills

- **ftrl**: FTRL bestemmelser that have vilkår
- **medlemskap**: Membership periods require fulfilled vilkår
- **behandling**: Treatment lifecycle including vilkår evaluation
- **behandlingsresultat**: Results include vilkår outcomes
