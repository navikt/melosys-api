# AvklarteFakta Reference

## Overview

AvklarteFakta (clarified facts) are pieces of information that need to be gathered/confirmed before a saksbehandler can evaluate vilkår. They come from external sources (PDL, Aareg, etc.) or manual input.

## AvklarteFaktaForBestemmelse

Routes to theme-specific fact requirements:

```kotlin
@Component
class AvklarteFaktaForBestemmelse(
    val avklarteFaktaYrkesaktiv: AvklarteFaktaYrkesaktiv,
    val avklarteFaktaIkkeYrkesaktiv: AvklarteFaktaIkkeYrkesaktiv,
    val avklarteFaktaPensjonist: AvklarteFaktaPensjonist
) {
    fun hentAvklarteFakta(
        bestemmelse: Bestemmelse,
        behandlingstema: Behandlingstema
    ): List<AvklartFakta> {
        return when (behandlingstema) {
            Behandlingstema.YRKESAKTIV -> avklarteFaktaYrkesaktiv.hentFakta(bestemmelse)
            Behandlingstema.IKKE_YRKESAKTIV -> avklarteFaktaIkkeYrkesaktiv.hentFakta(bestemmelse)
            Behandlingstema.PENSJONIST -> avklarteFaktaPensjonist.hentFakta(bestemmelse)
            else -> emptyList()
        }
    }
}
```

## Common AvklartFakta Types

| AvklartFakta | Description | Source |
|--------------|-------------|--------|
| `ARBEIDSFORHOLD` | Employment relationships | Aareg |
| `STATSBORGERSKAP` | Citizenship(s) | PDL |
| `PENSJON` | Pension information | Manual/Sigrun |
| `INNTEKT` | Income data | Sigrun |
| `MEDLEMSKAPSHISTORIKK` | Previous membership | MEDL |
| `BOSTED` | Residence information | PDL |
| `FAMILIEFORHOLD` | Family relationships | PDL |

## Facts per Behandlingstema

### YRKESAKTIV

Required facts for employed persons abroad:
- `ARBEIDSFORHOLD` - Current and historical employment
- `STATSBORGERSKAP` - For citizenship vilkår
- `INNTEKT` - For trygdeavgift calculation
- `BOSTED` - Current residence

### IKKE_YRKESAKTIV

Required facts for non-employed persons:
- `STATSBORGERSKAP`
- `MEDLEMSKAPSHISTORIKK` - Previous membership status
- `FAMILIEFORHOLD` - For "forsørget av medlem" vilkår
- `BOSTED`

### PENSJONIST

Required facts for pensioners:
- `STATSBORGERSKAP`
- `PENSJON` - Pension type and start date
- `MEDLEMSKAPSHISTORIKK`

## AvklartefaktaService

```java
@Service
public class AvklartefaktaService {

    // Fetches facts from external services
    public AvklarteFaktaResultat hentAvklarteFakta(
        Long behandlingId,
        Set<AvklartFakta> faktaTyper
    );

    // Stores manually entered facts
    public void lagreManuelleAvklarteFakta(
        Long behandlingId,
        AvklartFakta faktaType,
        String verdi
    );
}
```

## Fact Collection Flow

```
1. Bestemmelse selected
           │
           ▼
2. AvklarteFaktaForBestemmelse.hentAvklarteFakta()
           │
           ▼
3. System fetches facts from external services
           │
           ▼
4. Facts displayed in UI
           │
           ▼
5. Saksbehandler verifies/supplements facts
           │
           ▼
6. Vilkår evaluation can proceed
```

## Integration with Vilkår

The relationship between avklartefakta and vilkår:

| Vilkår | Required AvklartFakta |
|--------|----------------------|
| `NORSK_STATSBORGER` | `STATSBORGERSKAP` |
| `ANNEN_STATSBORGER` | `STATSBORGERSKAP` |
| `ARBEID_FOR_NORSK_ARBEIDSGIVER` | `ARBEIDSFORHOLD` |
| `TIDLIGERE_MEDLEM` | `MEDLEMSKAPSHISTORIKK` |
| `FORSØRGET_AV_MEDLEM` | `FAMILIEFORHOLD` |
| `MOTTOK_PENSJON_FØR_1994` | `PENSJON` |
| `OPPTJENINGSAAR_PENSJON` | `PENSJON`, `MEDLEMSKAPSHISTORIKK` |

## Manual vs Automatic Facts

### Automatic (from external services)
- Statsborgerskap from PDL
- Arbeidsforhold from Aareg
- Bosted from PDL

### Manual entry required
- Confirmation of employment for Norwegian employer abroad
- Pension details before 1994
- Specific work types (missionary, au pair, etc.)

## Database Storage

Facts are stored as part of the behandling context, not as separate entities. The evaluation results (what facts were used) appear in vilkaarsresultat begrunnelser.
