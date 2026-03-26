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
        land: Set<String>,
        statsborgerskap: Set<String>
    ): List<Vilkår> {
        return when (behandlingstema) {
            Behandlingstema.PENSJONIST -> vilkårForBestemmelsePensjonist.hentVilkår(bestemmelse, land, statsborgerskap)
            Behandlingstema.IKKE_YRKESAKTIV -> vilkårForBestemmelseIkkeYrkesaktiv.hentVilkår(bestemmelse, land, statsborgerskap)
            Behandlingstema.YRKESAKTIV -> vilkårForBestemmelseYrkesaktiv.hentVilkår(bestemmelse, land, statsborgerskap)
            else -> emptyList()
        }
    }
}
```

## Vilkår Data Class

```kotlin
data class Vilkår(
    val vilkår: Vilkaar,                              // The enum value
    val muligeBegrunnelser: Collection<String> = emptyList(), // Predefined reasons
    val defaultOppfylt: Boolean? = null               // Default evaluation value
)
```

### muligeBegrunnelser
Pre-defined reasons a saksbehandler can select when marking a vilkår. Common values:
- EØS country names for citizenship vilkår
- Employment types for work vilkår
- Pension types for pension vilkår

### defaultOppfylt
When set:
- `true`: Vilkår is pre-marked as fulfilled
- `false`: Vilkår is pre-marked as not fulfilled
- `null`: Saksbehandler must evaluate

## Vilkår per Bestemmelse

### FTRL_KAP2_2_8 (Frivillig medlemskap)

**YRKESAKTIV**:
| Vilkår | Description | Default |
|--------|-------------|---------|
| `NORSK_STATSBORGER` | Norwegian citizenship | null |
| `ANNEN_STATSBORGER` | Other EØS/third country | null |
| `TIDLIGERE_MEDLEM` | Was previously a member | null |
| `ARBEID_FOR_NORSK_ARBEIDSGIVER` | Works for Norwegian employer | null |
| `OPPTJENINGSAAR_PENSJON` | Has pension earning years | null |

**IKKE_YRKESAKTIV**:
| Vilkår | Description | Default |
|--------|-------------|---------|
| `NORSK_STATSBORGER` | Norwegian citizenship | null |
| `ANNEN_STATSBORGER` | Other EØS/third country | null |
| `TIDLIGERE_MEDLEM` | Was previously a member | null |
| `FORSØRGET_AV_MEDLEM` | Supported by a member | null |
| `OPPTJENINGSAAR_PENSJON` | Has pension earning years | null |

**PENSJONIST**:
| Vilkår | Description | Default |
|--------|-------------|---------|
| `NORSK_STATSBORGER` | Norwegian citizenship | null |
| `ANNEN_STATSBORGER` | Other EØS/third country | null |
| `TIDLIGERE_MEDLEM` | Was previously a member | null |
| `MOTTOK_PENSJON_FØR_1994` | Received pension before 1994 | null |

### FTRL_KAP2_2_5 (Pliktig medlemskap utland)

Each 2-5 variant has specific vilkår:

**FTRL_KAP2_2_5_FØRSTE_LEDD_A** (Statens tjenesteperson):
- `ARBEID_FOR_STAT` - Works for the state

**FTRL_KAP2_2_5_FØRSTE_LEDD_B** (Utenrikstjenesten):
- `ARBEID_FOR_NORSK_UTENRIKSTJENESTE` - Works for foreign service

**FTRL_KAP2_2_5_FØRSTE_LEDD_F** (Misjonær):
- `ARBEID_SOM_MISJONÆR` - Works as missionary

**FTRL_KAP2_2_5_FØRSTE_LEDD_G** (Au pair):
- `ARBEID_SOM_AU_PAIR` - Works as au pair

**FTRL_KAP2_2_5_FØRSTE_LEDD_H** (Student):
- `STUDERER_I_UTLANDET` - Studying abroad

## Vilkaarsresultat Entity

```java
@Entity
@Table(name = "VILKAARSRESULTAT")
public class Vilkaarsresultat implements Identifiable<Long> {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "BEHANDLINGSRESULTAT_ID")
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "VILKAAR")
    private Vilkaar vilkaar;

    @Column(name = "OPPFYLT")
    private Boolean oppfylt;

    @ElementCollection
    @CollectionTable(name = "VILKAARSRESULTAT_BEGRUNNELSER")
    private Set<String> begrunnelser = new HashSet<>();

    @Column(name = "BEGRUNNELSE_FRITEKST")
    private String begrunnelseFritekst;
}
```

## Database Schema

```sql
CREATE TABLE vilkaarsresultat (
    id                     NUMBER PRIMARY KEY,
    behandlingsresultat_id NUMBER NOT NULL REFERENCES behandlingsresultat(id),
    vilkaar                VARCHAR2(100) NOT NULL,
    oppfylt                NUMBER(1),  -- 0/1 boolean
    begrunnelse_fritekst   VARCHAR2(4000)
);

CREATE TABLE vilkaarsresultat_begrunnelser (
    vilkaarsresultat_id NUMBER NOT NULL REFERENCES vilkaarsresultat(id),
    begrunnelse         VARCHAR2(255)
);
```

## Adding New Vilkår

1. Add enum value to `Vilkaar.java`
2. Add to appropriate `VilkårForBestemmelse*` class
3. Create Flyway migration for any new database columns
4. Update frontend to display new vilkår
