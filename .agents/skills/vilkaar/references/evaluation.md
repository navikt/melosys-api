# Vilkår Evaluation Reference

## Evaluation Flow

```
                    Bestemmelse Selected
                            │
                            ▼
              VilkårForBestemmelse.hentVilkår()
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
          Select begrunnelse      Select begrunnelse
          (optional)              (required usually)
                │                       │
                └───────────┬───────────┘
                            ▼
              VilkaarsvurderingService.lagreVilkår()
                            │
                            ▼
              Vilkaarsresultat entities created
```

## VilkaarsvurderingService

```java
@Service
public class VilkaarsvurderingService {

    // Save vilkår evaluation results
    public void lagreVilkaarsvurdering(
        Long behandlingId,
        List<VilkaarsvurderingRequest> vurderinger
    );

    // Get current vilkår status for behandling
    public List<VilkaarsvurderingResponse> hentVilkaarsvurdering(
        Long behandlingId
    );

    // Check if all required vilkår are fulfilled
    public boolean alleVilkårOppfylt(Long behandlingId);
}
```

## VilkaarsvurderingRequest DTO

```java
public class VilkaarsvurderingRequest {
    private Vilkaar vilkaar;
    private Boolean oppfylt;
    private Set<String> begrunnelser;
    private String begrunnelseFritekst;
}
```

## Evaluation Rules

### All Vilkår Must Be Evaluated

Before proceeding to vedtak, all required vilkår must have:
- `oppfylt` set to true or false
- At least one begrunnelse if `oppfylt = false`

### Citizenship Vilkår (Mutual Exclusion)

Only one of these should be `oppfylt = true`:
- `NORSK_STATSBORGER`
- `ANNEN_STATSBORGER`

### Conditional Vilkår

Some vilkår only appear based on previous answers:

```kotlin
// Example: If ANNEN_STATSBORGER is selected, additional vilkår may appear
if (statsborgerskapInkludererEØS(statsborgerskap)) {
    vilkårListe.add(Vilkår(Vilkaar.EØS_BORGER_VILKÅR))
}
```

## Validation

### Pre-vedtak Validation

```kotlin
fun validerVilkårFørVedtak(behandlingId: Long) {
    val vilkårsresultater = hentVilkårsresultater(behandlingId)
    val påkrevdeVilkår = hentPåkrevdeVilkår(behandlingId)

    val manglende = påkrevdeVilkår - vilkårsresultater.map { it.vilkaar }
    if (manglende.isNotEmpty()) {
        throw FunksjonellException("Mangler evaluering av vilkår: $manglende")
    }

    val ikkeBegrunnet = vilkårsresultater
        .filter { !it.oppfylt && it.begrunnelser.isEmpty() }
    if (ikkeBegrunnet.isNotEmpty()) {
        throw FunksjonellException("Avslåtte vilkår mangler begrunnelse")
    }
}
```

## Begrunnelser

### Predefined Begrunnelser

Loaded from `VilkårForBestemmelse*` classes:

```kotlin
Vilkår(
    vilkår = Vilkaar.ANNEN_STATSBORGER,
    muligeBegrunnelser = listOf(
        "EØS-borger",
        "Tredjelandsborger med lovlig opphold",
        "Tredjelandsborger fra avtaleland"
    )
)
```

### Fritekst Begrunnelse

Always available for additional explanation:
- Required for non-standard cases
- Stored in `begrunnelse_fritekst` column

## Overriding Default Values

When vilkår has `defaultOppfylt`:

```kotlin
// In VilkårForBestemmelseYrkesaktiv
Vilkår(
    vilkår = Vilkaar.ARBEID_I_UTLANDET,
    defaultOppfylt = true  // Pre-checked in UI
)
```

Saksbehandler can override but must provide begrunnelse.

## Integration Points

### Stegvelger (Step Selection)

Vilkår status affects which steps are available:

```kotlin
if (!alleVilkårOppfylt(behandlingId)) {
    // Cannot proceed to certain steps
    throw FunksjonellException("Vilkår må være vurdert før dette steget")
}
```

### Behandlingsresultat

Vilkår results are attached to behandlingsresultat:

```java
@OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL)
private Set<Vilkaarsresultat> vilkaarsresultater = new HashSet<>();
```

### Medlemskapsperiode Generation

Vilkår outcomes influence period generation:

```kotlin
if (vilkaarOppfylt(Vilkaar.TIDLIGERE_MEDLEM)) {
    // May affect period start date logic
}
```

## Error Messages

| Error | Cause |
|-------|-------|
| "Vilkår X er påkrevd for bestemmelse Y" | Missing vilkår evaluation |
| "Begrunnelse mangler for avslått vilkår" | oppfylt=false without begrunnelse |
| "Kan ikke fatte vedtak før alle vilkår er vurdert" | Incomplete evaluation |
| "Ugyldig kombinasjon av vilkår" | Mutually exclusive vilkår both true |
