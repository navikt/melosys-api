# Vilkår Evaluation Reference

## Evaluation Flow

```
                    Bestemmelse + behandling selected
                            │
                            ▼
       VilkårForBestemmelse.hentVilkår(bestemmelse, behandlingstema, avklarteFakta, behandlingID)
                            │
                            ▼
              List<Vilkår> returned to frontend
                            │
                            ▼
              Saksbehandler evaluates each vilkår
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
          oppfylt = true         oppfylt = false
                │                       │
                ▼                       ▼
          Select begrunnelse-kode(r) / fritekst (optional)
                │                       │
                └───────────┬───────────┘
                            ▼
       POST /vilkaar/{behandlingID}  →  VilkaarsresultatService.registrerVilkår(...)
                            │
                            ▼
              Vilkaarsresultat (+ VilkaarBegrunnelse) entities created
```

## Save / read path

The REST entry point is `VilkaarController` (`frontend-api/.../tjenester/gui/VilkaarController.java`, base path `/vilkaar`):

- `GET  /vilkaar/{behandlingID}` → `VilkaarsresultatService.hentVilkaar(behandlingID): List<VilkaarDto>`
- `POST /vilkaar/{behandlingID}` (body `List<VilkaarDto>`) → `VilkaarsresultatService.registrerVilkår(behandlingID, vilkaarDtoer)`, then returns the refreshed list
- `PUT  /vilkaar/{behandlingID}/inngangsvilkaar/overstyr` → `InngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(behandlingID)`

`VilkaarsresultatService` lives at `service/.../behandling/VilkaarsresultatService.kt`. Relevant methods:

```kotlin
fun hentVilkaar(behandlingID: Long): List<VilkaarDto>                 // read
fun registrerVilkår(behandlingID: Long, vilkaarDtoer: List<VilkaarDto>) // save (validates, resets, re-inserts)
fun oppdaterVilkaarsresultat(behandlingID, vilkaar, oppfylt, begrunnelseKoder: Set<Kodeverk>)
fun finnVilkaarsresultat(behandlingID, vilkaar: Vilkaar): Vilkaarsresultat?
fun oppfyllerVilkaar(behandlingID, vilkaar: Vilkaar): Boolean
fun harVilkaar(behandlingID, vilkaar: List<Vilkaar>): Boolean
fun harVilkaarForUtsending(behandlingID): Boolean
fun harVilkaarForUnntak(behandlingID): Boolean
```

`registrerVilkår` rejects changes to `IMMUTABLE_VILKAAR` (currently `FO_883_2004_INNGANGSVILKAAR`) with a `FunksjonellException`. For EØS-saker it keeps the immutable vilkår and only resets the rest; otherwise it clears all vilkårsresultater before re-inserting.

## VilkaarDto

```java
public class VilkaarDto {
    private String vilkaar;                  // Vilkaar.kode
    private Boolean oppfylt = false;
    private Set<String> begrunnelseKoder;    // VilkaarBegrunnelse.kode values
    private String begrunnelseFritekst;
    private String begrunnelseFritekstEngelsk; // maps to begrunnelseFritekstEessi
}
```

## Begrunnelser

### Predefined begrunnelse-koder

Set on the `Vilkår.muligeBegrunnelser` list from begrunnelse-kodeverk. Example from the router:

```kotlin
Vilkår(
    FTRL_2_7_RIMELIGHETSVURDERING,
    muligeBegrunnelser = toStringList(*Ftrl_2_7_begrunnelser.values())
)
```

On save, the chosen `begrunnelseKoder` are stored as `VilkaarBegrunnelse` rows (`kode`) on the `Vilkaarsresultat`.

### Fritekst begrunnelse

`begrunnelseFritekst` (and `begrunnelseFritekstEessi`, exposed as `begrunnelseFritekstEngelsk` in the DTO) hold free-text explanation, stored on `vilkaarsresultat`.

## defaultOppfylt

A `Vilkår` may carry `defaultOppfylt = true` (e.g. `FTRL_2_5_MEDFØLGENDE_A_E` for ektefelle-relasjon), pre-marking it as fulfilled in the UI. Saksbehandler can still override.

## Behandlingsresultat link

Vilkår results hang off behandlingsresultat (1:1 with behandling):

```java
@OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL)
private Set<Vilkaarsresultat> vilkaarsresultater = new HashSet<>();
```

Each `Vilkaarsresultat.behandlingsresultat` is mapped via `@JoinColumn(name = "beh_resultat_id")`.
