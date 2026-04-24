# Plan: Opprydding av avrunding i trygdeavgiftsbeløp

## Bakgrunn

`melosys-trygdeavgift-beregning` returnerte tidligere alltid hele kroner.
Etter endring i beregningsmodulen kan den nå returnere desimalbeløp.
Denne PR-en (7588) har derfor måttet innføre avrunding på flere steder
for å unngå feil i nedstrøms systemer. Avrundingen er nå spredd utover
kodebasen og bør konsolideres.

## Nåsituasjon: Avrundinger innført i denne PR-en

| # | Fil | Kode | Hvorfor innført |
|---|-----|------|-----------------|
| 1 | `FakturabeskrivelseMapper.kt:30` | `.setScale(0, RoundingMode.HALF_UP)` | Faktureringskomponenten kaster `ArithmeticException` ved desimaler |
| 2 | `ÅrsavregningController.kt:228` | `.setScale(0, HALF_UP).intValueExact()` | Frontend DTO krever `Int`, desimaler ga feil |
| 3 | `SendFakturaÅrsavregning.kt:85` | `.setScale(2, RoundingMode.HALF_UP)` | Faktureringskomponenten godtar maks 2 desimaler |

## Nåsituasjon: Eksisterende avrundinger fra master som nå er risikable

Disse fantes allerede, men var ufarlige fordi beregningsmodulen alltid
returnerte hele tall. Med desimaler fra beregningsmodulen er de nå latente
feilkilder:

| # | Fil | Kode | Risiko |
|---|-----|------|--------|
| 4 | `TrygdeavgiftsperiodeDto.kt:28` | `.toInt()` | Trunkerer — gir feil resultat ved desimaler (f.eks. 799.7 → 799) |
| 5 | `EøsPensjonistTrygdeavgiftsperiodeDto.kt:22` | `.toInt()` | Samme problem |
| 6 | `SendPensjonsopptjeningHendelse.kt:133` | `.toLong()` | Samme problem |

## Plan

### Steg 1: Avrund ved kilden

Innfør `Penger.avrundTilHelKroner()` og bruk den i de to
beregningsservicene — de eneste stedene der `trygdeavgiftsbeløpMd`
settes fra beregningsmodulen.

**Ny funksjon i `Penger.kt`:**

```kotlin
fun avrundTilHelKroner(): Penger =
    Penger(verdi?.setScale(0, java.math.RoundingMode.DOWN), valuta)
```

**Endring i `TrygdeavgiftsberegningService.kt:193` og
`EøsPensjonistTrygdeavgiftsberegningService.kt:156`:**

```kotlin
// Før:
trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
// Etter:
trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger().avrundTilHelKroner(),
```

**Begrunnelse:** `RoundingMode.DOWN` (trunkering mot null) samsvarer
med Skatteetatens praksis for trygdeavgift. Ved å plassere funksjonen
på `Penger`-klassen er avrundingsregelen definert på nøyaktig ett sted.
Ved å bruke den i beregningsservicene — der beløpet mottas fra den
eksterne beregningsmodulen — garanterer vi at resten av domenemodellen
opererer med hele kroner, akkurat som før endringen i beregningsmodulen.
Alle nedstrøms konsumenter (fakturering, frontend, brev,
pensjonsopptjening, årsavregning) får hele tall uten å vite om
avrunding.

### Steg 2: Fjern all nedstrøms avrunding innført i denne PR-en

Etter steg 1 er `trygdeavgiftsbeløpMd` garantert et helt tall.
Avrunding lenger ned i kjeden er overflødig og tilslører hvor den
faktiske avrundingen skjer. Alle fire avrundinger innført i denne
PR-en fjernes:

**`FakturabeskrivelseMapper.kt:30`:**

```kotlin
// Før:
enhetsprisPerManed = periode.trygdeavgiftsbeløpMd.hentVerdi().setScale(0, RoundingMode.HALF_UP),
// Etter:
enhetsprisPerManed = periode.trygdeavgiftsbeløpMd.hentVerdi(),
```

**`ÅrsavregningController.kt:228`:**

```kotlin
// Før:
avgiftPerMd = periode.trygdeavgiftsbeløpMd.hentVerdi().setScale(0, java.math.RoundingMode.HALF_UP).intValueExact()
// Etter:
avgiftPerMd = periode.trygdeavgiftsbeløpMd.hentVerdi().intValueExact()
```

**`SendFakturaÅrsavregning.kt:85`:**

```kotlin
// Før:
belop = årsavregning.hentTilFaktureringBeloep.setScale(2, RoundingMode.HALF_UP),
// Etter:
belop = årsavregning.hentTilFaktureringBeloep,
```

**Begrunnelse:** Ingen av disse avrundingene fantes på master. De ble
innført i denne PR-en som nødløsninger fordi beregningsmodulen begynte
å returnere desimaler. Etter steg 1 er input identisk med master:
`trygdeavgiftsbeløpMd` er et helt tall, og `tilFaktureringBeloep`
beregnes via `TotalbeløpBeregner` som internt håndterer presisjon med
`.setScale(2, HALF_UP)`. Å beholde avrundingene ville gi inntrykk av
at beløpene kan ha desimaler på disse tidspunktene, noe som ikke
stemmer.

### Steg 3: Erstatt stille trunkering med defensive `Exact`-varianter

Tre steder fra master bruker `.toInt()` / `.toLong()` for å konvertere
`trygdeavgiftsbeløpMd` til heltall. Disse trunkerer stille ved
desimaler (f.eks. 799.7 → 799). Erstatt med `Exact`-varianter:

- `TrygdeavgiftsperiodeDto.kt:28` — `.toInt()` → `.intValueExact()`
- `EøsPensjonistTrygdeavgiftsperiodeDto.kt:22` — `.toInt()` → `.intValueExact()`
- `SendPensjonsopptjeningHendelse.kt:133` — `.toLong()` → `.longValueExact()`

**Begrunnelse:** Etter steg 1 er verdien alltid et helt tall, så
`Exact`-variantene gir identisk resultat i normalflyt. Men de fungerer
som sikkerhetsventil: dersom noen i fremtiden endrer kilden uten å
avrunde, kaster de `ArithmeticException` i stedet for å gi stille
feil data. På master var `.toInt()` ufarlig fordi beregningsmodulen
alltid returnerte hele tall — men det var en implisitt antagelse som
nå er brutt. `Exact`-varianter gjør antagelsen eksplisitt.

## Oppsummering

| Steg | Handling | Netto effekt |
|------|----------|-------------|
| 1 | Avrund ved kilden (ny funksjon + 2 filer) | All nedstrøms kode får hele kroner |
| 2 | Fjern nedstrøms avrunding (3 filer) | Fjerner 4 overflødige avrundinger |
| 3 | `.toInt()`→`.intValueExact()` (3 filer) | Defensiv sikring |

**Resultat:** Avrunding av `trygdeavgiftsbeløpMd` skjer på nøyaktig
ett sted i kodebasen. Alle fire overflødige avrundinger innført i
denne PR-en fjernes. Tre stille trunkeringer erstattes med defensive
`Exact`-varianter.
