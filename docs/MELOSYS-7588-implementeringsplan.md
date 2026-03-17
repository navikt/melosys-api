# MELOSYS-7588 + MELOSYS-7969: Utvid datamodell for trygdeavgiftsperioder

**Dato:** 2026-03-17 (oppdatert)
**Oppgaver:**
- [MELOSYS-7588](https://jira.adeo.no/browse/MELOSYS-7588) — Utvid datamodell til å støtte flere grunnlagsperioder
- [MELOSYS-7969](https://jira.adeo.no/browse/MELOSYS-7969) — Lagre beregningstype (25%-regel/minstebeløp) og gjør sats nullable

**Epic:** [MELOSYS-7464](https://jira.adeo.no/browse/MELOSYS-7464) — Støtte til 25%-regelen og minstebeløpet
**Teknisk analyse:** [MELOSYS-7557](https://jira.adeo.no/browse/MELOSYS-7557)
**Confluence:** [Eksempler på fastsettelse av trygdeavgift](https://confluence.adeo.no/spaces/TEESSI/pages/535938349) | [25%-regelen](https://confluence.adeo.no/spaces/TEESSI/pages/704156896) | [Fysisk DB-modell](https://confluence.adeo.no/spaces/TEESSI/pages/603722350)

> **Hvorfor slått sammen?** Begge oppgavene endrer nøyaktig de samme filene på nøyaktig de samme stedene
> (API-kontrakten, `.last()`-metodene, entiteten, DTO-ene, brevmapperne). Å gjøre dem separat ville
> betydd to API-kontraktendringer, to deploy-koordineringer, og dobbel omskriving av alle tester.

---

## 1. Problemet

### Dagens modell (1:1)

Hver `Trygdeavgiftsperiode` har **nøyaktig ett** grunnlag — fem FK-felter der kun ett av de tre avgiftspliktig-typene er satt:

```mermaid
erDiagram
    Trygdeavgiftsperiode ||--o| Medlemskapsperiode : "grunnlagMedlemskapsperiode"
    Trygdeavgiftsperiode ||--o| Lovvalgsperiode : "grunnlagLovvalgsPeriode"
    Trygdeavgiftsperiode ||--o| HelseutgiftDekkesPeriode : "grunnlagHelseutgiftDekkesPeriode"
    Trygdeavgiftsperiode ||--|| Inntektsperiode : "grunnlagInntekstperiode"
    Trygdeavgiftsperiode ||--|| SkatteforholdTilNorge : "grunnlagSkatteforholdTilNorge"

    Trygdeavgiftsperiode {
        Long id PK
        LocalDate periodeFra
        LocalDate periodeTil
        Penger trygdeavgiftsbelopMd
        BigDecimal trygdesats
    }
```

**Begrensning i kode:**
- `Trygdeavgiftsperiode.addGrunnlag()` (linje 117) kaster error ved mer enn ett grunnlag: `"Kan ikke ha flere grunnlag samtidig."`
- `BeregningService.kt` i `melosys-trygdeavgift-beregning` bruker `.last()` på 3 steder + 1 i `EøsPensjonistBeregningService.kt` — kun siste grunnlag bevares når 25%-regelen slår inn

### Manglende metadata om beregningstype (7969)

I tillegg til grunnlag-problemet, mangler det informasjon om **hvilken beregningsregel** som ble brukt:

1. **Ingen indikator på 25%-regel vs ordinær beregning** — verken i API-responsen fra `melosys-trygdeavgift-beregning` eller i `Trygdeavgiftsperiode`-entiteten. Saksbehandler og brev kan ikke se *hvorfor* en periode har den satsen den har.
2. **Sats `0.00` brukes som proxy** — de 4 `.last()`-stedene setter `sats = BigDecimal.ZERO.setScale(2)` for begrensede perioder. Men `0%` er feil — 25%-regelen bruker ingen sats, den beregner et totalbeløp. Sats bør være `null`.
3. **Brevmappere sjekker `trygdesats == BigDecimal.ZERO`** — `InnvilgelseFtrlMapper.kt` (linje 301, 334) og `InformasjonTrygdeavgiftMapper.kt` (linje 74) bruker dette for å avgjøre om avgiftsperioder skal vises. Med nullable sats må denne logikken oppdateres.

### Hva 25%-regelen krever

Når 25%-regelen gir gunstigere beregning, erstattes **flere** ordinære trygdeavgiftsperioder med **én** samlet periode (se [Confluence-eksemplene](https://confluence.adeo.no/spaces/TEESSI/pages/535938349)).

Fra MELOSYS-7557: *"En 'lang' trygdeavgiftsperiode med beløpet fra 25%-regelen erstatter mindre trygdeavgiftsperioder når totalbeløpet for året overstiger grensen."*

**Eksempel (Confluence Eksempel 2):** En frivillig medlem med utenlandsk inntekt 20 000→25 000 kr/mnd og næringsinntekt 10 000 kr/mnd. Pensjonsdelen beregnes etter 25%-regelen og gir **én** samlet trygdeavgiftsperiode som baserer seg på **5 underliggende inntektsperioder med ulik sats og beløp**.

Denne ene perioden trenger altså å referere **alle** de opprinnelige grunnlagene — ikke bare det siste.

---

## 2. Målbilde (1:N)

```mermaid
erDiagram
    Trygdeavgiftsperiode ||--o{ TrygdeavgiftsperiodeGrunnlag : "grunnlag"
    TrygdeavgiftsperiodeGrunnlag ||--o| Medlemskapsperiode : "avgiftspliktigperiode"
    TrygdeavgiftsperiodeGrunnlag ||--o| Lovvalgsperiode : "avgiftspliktigperiode"
    TrygdeavgiftsperiodeGrunnlag ||--o| HelseutgiftDekkesPeriode : "avgiftspliktigperiode"
    TrygdeavgiftsperiodeGrunnlag ||--|| Inntektsperiode : "inntektsperiode"
    TrygdeavgiftsperiodeGrunnlag ||--|| SkatteforholdTilNorge : "skatteforhold"

    Trygdeavgiftsperiode {
        Long id PK
        LocalDate periodeFra
        LocalDate periodeTil
        Penger trygdeavgiftsbelopMd
        BigDecimal trygdesats "nullable (7969)"
        Avgiftsberegningstype beregningstype "ny (7969)"
    }

    TrygdeavgiftsperiodeGrunnlag {
        Long id PK
        Long trygdeavgiftsperiode_id FK
        Long medlemskapsperiode_id FK "nullable"
        Long lovvalgsperiode_id FK "nullable"
        Long helseutgift_dekkes_periode_id FK "nullable"
        Long inntektsperiode_id FK
        Long skatteforhold_id FK
    }
```

**Merk:** Navnet `TrygdeavgiftsperiodeGrunnlag` er valgt for å unngå navnekollisjon med `AvgiftsperiodeGrunnlag` som allerede eksisterer i `melosys-trygdeavgift-beregning`.

### Ny enum: `Avgiftsberegningstype` (7969)

```kotlin
enum class Avgiftsberegningstype {
    ORDINAER,              // Vanlig satsberegning
    TJUEFEM_PROSENT_REGEL, // 25%-regel ga gunstigere beregning
    MINSTEBELOEP           // Inntekt under minstebeløpet → ingen avgift
}
```

---

## 3. Endringer per repo og koordinering

### Sekvens mellom repoene

```mermaid
sequenceDiagram
    participant API as melosys-api
    participant BEREGN as melosys-trygdeavgift-beregning

    Note over API,BEREGN: Fase 1: API-kontraktendring

    API->>BEREGN: POST /api/v2/beregn (request uendret)
    BEREGN->>BEREGN: Beregn avgift, inkl. 25%-regel

    Note over BEREGN: I DAG: grunnlag = AvgiftsperiodeGrunnlag(perioder.last()), sats = 0.00
    Note over BEREGN: NYTT: grunnlag = List[alle perioder], sats = null, beregningstype = 25%

    BEREGN-->>API: List<BeregnetTrygdeavgiftResponse>

    Note over API: Mottar N grunnlag + beregningstype per respons-element
    API->>API: lagTrygdeavgiftsperiode(): mapper grunnlag + beregningstype + nullable sats
    API->>API: Lagrer via JPA cascade
```

**Deploy-rekkefølge:** `melosys-trygdeavgift-beregning` MÅ deployes **før** eller **samtidig med** `melosys-api`, fordi API-et må kunne lese den nye JSON-strukturen. Alternativt: gjør endringen bakoverkompatibel med en overgangsperiode.

---

## 4. Detaljert endringsplan

### Fase 1: melosys-trygdeavgift-beregning — API-kontraktendring

#### Filer som endres

| Fil | Endring (7588) | Endring (7969) |
|-----|----------------|----------------|
| `modell/felles/Trygdeavgiftsperiode.kt` | — | `sats: BigDecimal` → `sats: BigDecimal?` (nullable) |
| `modell/felles/Avgiftsberegningstype.kt` | — | **Ny enum:** `ORDINAER`, `TJUEFEM_PROSENT_REGEL`, `MINSTEBELOEP` |
| `standard/modell/BeregnetTrygdeavgiftResponse.kt` | `grunnlag` → `List<AvgiftsperiodeGrunnlag>` | Nytt felt: `beregningstype: Avgiftsberegningstype` |
| `standard/modell/AvgiftsperiodeGrunnlag.kt` | Uendret (brukes fortsatt for enkeltelement) | — |
| `standard/BeregningService.kt` | 3 steder: `.last()` → pass hele listen | 3 steder: `sats = BigDecimal.ZERO.setScale(2)` → `sats = null` + sett beregningstype |
| `eospensjonist/modell/EøsPensjonistBeregnetTrygdeavgiftResponse.kt` | `grunnlag` → `List<...>` | Nytt felt: `beregningstype: Avgiftsberegningstype` |
| `eospensjonist/EøsPensjonistBeregningService.kt` | 1 sted: `.last()` → pass hele listen | 1 sted: `sats = null` + beregningstype |
| Berørte tester | Alle tester som konstruerer `BeregnetTrygdeavgiftResponse` | Tester som asserted `sats shouldBe BigDecimal.ZERO` → `sats shouldBe null` |

#### Detalj: Endringer i de 4 begrensningsmetodene (7588 + 7969 kombinert)

Hver av de 4 metodene har **to** problemer som fikses samtidig:

**1. `opprettBegrensetAvgiftResponse` (pliktig, linje 368-377)**
```kotlin
// FØR:
BeregnetTrygdeavgiftResponse(
    Trygdeavgiftsperiode(periode = samletPeriode, sats = BigDecimal.ZERO.setScale(2), ...),
    AvgiftsperiodeGrunnlag(grunnlagPerioder.last())
)
// ETTER:
BeregnetTrygdeavgiftResponse(
    Trygdeavgiftsperiode(periode = samletPeriode, sats = null, ...),  // 7969: null i stedet for 0
    grunnlagPerioder.map { AvgiftsperiodeGrunnlag(it) },              // 7588: alle grunnlag
    beregningstype = Avgiftsberegningstype.TJUEFEM_PROSENT_REGEL      // 7969: eksplisitt type
)
```

**2. `opprettBegrensetResponse` (frivillig helse/pensjon, linje 267-276)**
```kotlin
// FØR:
opprettBegrensetAvgiftResponseForPeriode(..., grunnlagPeriode = avgiftsgrunnlagPerioder.last())
// ETTER:
opprettBegrensetAvgiftResponseForPeriode(..., grunnlagPerioder = avgiftsgrunnlagPerioder)
// + sats = null, beregningstype = TJUEFEM_PROSENT_REGEL
```

**3. `opprettMisjonærBegrensetAvgiftResponse` (misjonær, linje 132-147)**
```kotlin
// FØR:
grunnlagPeriode = periodeberegninger.last().grunnlagPeriode
// ETTER:
grunnlagPerioder = periodeberegninger.map { it.grunnlagPeriode }
// + sats = null, beregningstype = TJUEFEM_PROSENT_REGEL
```

**4. `EøsPensjonistBeregningService.opprettBegrensetAvgiftResponse` (linje 85-96)**
```kotlin
// Samme mønster — .last() → .map { ... }, sats = null, beregningstype
```

**Minstebeløp-tilfellet** (inntekt under minstebeløpet → ingen avgift):
Trenger også `beregningstype = MINSTEBELOEP` med `sats = null` og `månedsavgift = 0`.
Sjekk om dette håndteres i beregningsservicen allerede eller om det bare filtreres bort.

#### Ny JSON-kontrakt (respons)

```json
[
  {
    "beregnetPeriode": {
      "periode": { "fom": "2025-05-01", "tom": "2025-12-31" },
      "sats": 7.7,
      "månedsavgift": { "verdi": 1540, "valuta": { "kode": "NOK", "desimaler": 2 } }
    },
    "grunnlag": [
      {
        "medlemskapsperiodeId": "uuid-1",
        "skatteforholdsperiodeId": "uuid-a",
        "inntektsperiodeId": "uuid-x"
      }
    ],
    "beregningstype": "ORDINAER"
  },
  {
    "beregnetPeriode": {
      "periode": { "fom": "2025-05-01", "tom": "2025-12-31" },
      "sats": null,
      "månedsavgift": { "verdi": 3448, "valuta": { "kode": "NOK", "desimaler": 2 } }
    },
    "grunnlag": [
      {
        "medlemskapsperiodeId": "uuid-1",
        "skatteforholdsperiodeId": "uuid-a",
        "inntektsperiodeId": "uuid-x"
      },
      {
        "medlemskapsperiodeId": "uuid-1",
        "skatteforholdsperiodeId": "uuid-b",
        "inntektsperiodeId": "uuid-y"
      }
    ],
    "beregningstype": "TJUEFEM_PROSENT_REGEL"
  }
]
```

**Kontrakt-endringer oppsummert (7588 + 7969):**
- `grunnlag`: `object` → `array` (alltid liste, 1 element for ordinær beregning)
- `sats`: `number` → `number | null` (null ved 25%-regel og minstebeløp)
- `beregningstype`: **nytt felt** — `ORDINAER`, `TJUEFEM_PROSENT_REGEL`, eller `MINSTEBELOEP`

**Bakoverkompatibilitet:** Ordinære perioder har `grunnlag` med 1 element, `sats` med verdi, og `beregningstype = ORDINAER`. melosys-api kan håndtere begge formater under overgangsperioden ved å sjekke om `grunnlag` er array eller objekt.

---

### Fase 2: melosys-api — Ny entitet og Flyway-migrasjon

#### Ny entitet: `TrygdeavgiftsperiodeGrunnlag.kt`

**Plassering:** `domain/src/main/kotlin/no/nav/melosys/domain/avgift/TrygdeavgiftsperiodeGrunnlag.kt`

```kotlin
@Entity
@Table(name = "trygdeavgiftsperiode_grunnlag")
class TrygdeavgiftsperiodeGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trygdeavgiftsperiode_id", nullable = false)
    var trygdeavgiftsperiode: Trygdeavgiftsperiode,

    @ManyToOne
    @JoinColumn(name = "medlemskapsperiode_id")
    var medlemskapsperiode: Medlemskapsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "lovvalgsperiode_id")
    var lovvalgsperiode: Lovvalgsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "helseutgift_dekkes_periode_id")
    var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "inntektsperiode_id")
    val inntektsperiode: Inntektsperiode,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "skatteforhold_id")
    val skatteforhold: SkatteforholdTilNorge,
)
```

#### Flyway-migrasjon: `V150__trygdeavgiftsperiode_grunnlag_og_beregningstype.sql`

```sql
-- 1. Ny grunnlag-tabell (7588)
CREATE TABLE trygdeavgiftsperiode_grunnlag (
    id                          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    trygdeavgiftsperiode_id     NUMBER NOT NULL,
    medlemskapsperiode_id       NUMBER,
    lovvalgsperiode_id          NUMBER,
    helseutgift_dekkes_periode_id NUMBER,
    inntektsperiode_id          NUMBER NOT NULL,
    skatteforhold_id            NUMBER NOT NULL,
    CONSTRAINT fk_tag_trygdeavgiftsperiode FOREIGN KEY (trygdeavgiftsperiode_id)
        REFERENCES trygdeavgiftsperiode(id),
    CONSTRAINT fk_tag_medlemskapsperiode FOREIGN KEY (medlemskapsperiode_id)
        REFERENCES medlemskapsperiode(id),
    CONSTRAINT fk_tag_lovvalgsperiode FOREIGN KEY (lovvalgsperiode_id)
        REFERENCES lovvalgsperiode(id),
    CONSTRAINT fk_tag_helseutgift FOREIGN KEY (helseutgift_dekkes_periode_id)
        REFERENCES helseutgift_dekkes_periode(id),
    CONSTRAINT fk_tag_inntektsperiode FOREIGN KEY (inntektsperiode_id)
        REFERENCES inntektsperiode(id),
    CONSTRAINT fk_tag_skatteforhold FOREIGN KEY (skatteforhold_id)
        REFERENCES skatteforhold_til_norge(id)
);

-- 2. Migrer eksisterende grunnlag-data (7588)
INSERT INTO trygdeavgiftsperiode_grunnlag
    (trygdeavgiftsperiode_id, medlemskapsperiode_id, lovvalgsperiode_id,
     helseutgift_dekkes_periode_id, inntektsperiode_id, skatteforhold_id)
SELECT
    t.id, t.medlemskapsperiode_id, t.lovvalg_periode_id,
    t.helseutgift_dekkes_periode_id, t.inntektsperiode_id, t.skatteforhold_id
FROM trygdeavgiftsperiode t
WHERE t.inntektsperiode_id IS NOT NULL
  AND t.skatteforhold_id IS NOT NULL;

-- 3. Ny kolonne for beregningstype (7969)
ALTER TABLE trygdeavgiftsperiode ADD beregningstype VARCHAR2(30);

-- 4. Sett default beregningstype for eksisterende data (7969)
UPDATE trygdeavgiftsperiode SET beregningstype = 'ORDINAER';

-- 5. Gjør trygdesats nullable (7969)
-- I dag: "trygdesats NUMBER NOT NULL" — 25%-regel og minstebeløp har ingen sats
ALTER TABLE trygdeavgiftsperiode MODIFY trygdesats NULL;
```

**Separat migrasjon `V151__fjern_gamle_grunnlag_fk.sql`** (etter at all kode er oppdatert og deployet):
```sql
ALTER TABLE trygdeavgiftsperiode DROP COLUMN inntektsperiode_id;
ALTER TABLE trygdeavgiftsperiode DROP COLUMN skatteforhold_id;
ALTER TABLE trygdeavgiftsperiode DROP COLUMN medlemskapsperiode_id;
ALTER TABLE trygdeavgiftsperiode DROP COLUMN lovvalg_periode_id;
ALTER TABLE trygdeavgiftsperiode DROP COLUMN helseutgift_dekkes_periode_id;
```

---

### Fase 3: melosys-api — Oppdater Trygdeavgiftsperiode-entiteten

#### Endring i `Trygdeavgiftsperiode.kt`

**Fjernes (7588):**
- De 5 FK-feltene (`grunnlagMedlemskapsperiode`, `grunnlagLovvalgsPeriode`, `grunnlagHelseutgiftDekkesPeriode`, `grunnlagInntekstperiode`, `grunnlagSkatteforholdTilNorge`)
- `addGrunnlag()`-metoden
- Getter-metodene `hentGrunnlagMedlemskapsperiode()`, `hentGrunnlagInntekstperiode()`, `hentGrunnlagSkatteforholdTilNorge()`

**Endres (7969):**
```kotlin
// FØR:
@Column(name = "trygdesats", nullable = false)
val trygdesats: BigDecimal,

// ETTER:
@Column(name = "trygdesats")
val trygdesats: BigDecimal?,           // nullable — null ved 25%-regel/minstebeløp

@Column(name = "beregningstype")
@Enumerated(EnumType.STRING)
val beregningstype: Avgiftsberegningstype = Avgiftsberegningstype.ORDINAER,
```

**Legges til (7588):**
```kotlin
@OneToMany(mappedBy = "trygdeavgiftsperiode", cascade = [CascadeType.ALL], orphanRemoval = true)
val grunnlag: MutableList<TrygdeavgiftsperiodeGrunnlag> = mutableListOf()
```

**Nye hjelpemetoder:**
```kotlin
fun hentGrunnlagAvgiftsperiode(): AvgiftspliktigPeriode =
    grunnlag.firstOrNull()?.hentAvgiftspliktigperiode()
        ?: error("Ingen grunnlag på trygdeavgiftsperiode")

fun hentAlleGrunnlag(): List<TrygdeavgiftsperiodeGrunnlag> = grunnlag.toList()

fun leggTilGrunnlag(g: TrygdeavgiftsperiodeGrunnlag) {
    g.trygdeavgiftsperiode = this
    grunnlag.add(g)
}

fun erBegrenset(): Boolean = beregningstype != Avgiftsberegningstype.ORDINAER
```

**Oppdateres:**
- `copyEntity()` — kopierer grunnlag-listen + beregningstype
- `erLikForSatsendring()` — sammenligner grunnlag-lister + beregningstype; nullable sats-sammenligning
- `harAvgift()` — `trygdesats` er nå nullable: `trygdesats?.let { BigDecimal.ZERO.compareTo(it) != 0 } ?: false`

**Impact av nullable `trygdesats` (7969):** 48 filer refererer `trygdesats`. Alle steder som leser `trygdesats` direkte (`.toDouble()`, `.compareTo()`, string-interpolering `"Sats: ${it.trygdesats} %"`) må håndtere null-tilfellet:

| Brukssted | Dagens kode | Ny kode |
|-----------|-------------|---------|
| `TrygdeavgiftsperiodeDto.kt` | `trygdesats.toDouble()` | `trygdesats?.toDouble()` (DTO-feltet også nullable) |
| `ÅrsavregningController.kt` | `periode.trygdesats.toDouble()` | `periode.trygdesats?.toDouble()` |
| `OpprettFakturaserie.kt` | `"Sats: ${it.trygdesats} %"` | `trygdesats?.let { "Sats: $it %" } ?: "25%-regel"` |
| `InnvilgelseFtrlMapper.kt` | `it.trygdesats == BigDecimal.ZERO` | `it.trygdesats == null \|\| it.trygdesats == BigDecimal.ZERO` (eller bruk `erBegrenset()`) |
| `InformasjonTrygdeavgiftMapper.kt` | Samme mønster | Samme løsning |
| `ÅrsavregningVedtakMapper.kt` | `trygdesats` i brev | Vis "25%-regel" i stedet for sats |
| `erLikForSatsendring()` | `trygdesats.compareTo(...)` | Null-safe sammenligning |

---

### Fase 4: melosys-api — Oppdater service- og DTO-lag

#### Komplett liste over berørte filer

Diagrammet viser avhengighetsgrafen fra entiteten og ut:

```mermaid
graph TD
    A[Trygdeavgiftsperiode.kt<br/>ENTITET - 5 FK → 1:N grunnlag] --> B[TrygdeavgiftsberegningService.kt<br/>lagTrygdeavgiftsperiode]
    A --> C[EøsPensjonistTrygdeavgiftsberegningService.kt<br/>lagEøsTrygdeavgiftsperiode]
    A --> D[TrygdeavgiftperiodeErstatter.kt<br/>erstattTrygdeavgiftsperioder]
    A --> E[ReplikerBehandlingsresultatService.kt<br/>copyEntity + addGrunnlag]
    A --> F[LovvalgsperiodeService.kt<br/>copyEntity]
    A --> G[TrygdeavgiftMottakerService.kt<br/>leser grunnlag]
    A --> H[TotalbeløpBeregner.kt<br/>leser grunnlag]
    A --> I[ÅrsavregningIkkeSkattepliktigeFinner.kt<br/>leser grunnlag]

    A --> J[InnvilgelseFtrlMapper.kt<br/>hentGrunnlag* for brev]
    A --> K[ÅrsavregningVedtakMapper.kt<br/>hentGrunnlag* for brev]
    A --> L[InformasjonTrygdeavgiftMapper.kt<br/>hentGrunnlag* for brev]

    A --> M[TrygdeavgiftsperiodeDto.kt<br/>frontend-api DTO]
    A --> N[TrygdeavgiftsgrunnlagDto.kt<br/>frontend-api DTO]
    A --> O[EøsPensjonistTrygdeavgiftsperiodeDto.kt<br/>frontend-api DTO]
    A --> P[ÅrsavregningController.kt<br/>mapTilTrygdeavgiftperiodeDto]

    A --> Q[OpprettFakturaserie.kt<br/>saksflyt - inntekt for faktura]
    A --> R[BeregnOgSendFaktura.kt<br/>saksflyt - satsendring]

    A --> S[Inntektsperiode.java<br/>reverse @OneToMany]
    A --> T[SkatteforholdTilNorge.java<br/>reverse @OneToMany]

    style A fill:#f96,stroke:#333,stroke-width:2px
    style B fill:#ff9,stroke:#333
    style C fill:#ff9,stroke:#333
    style D fill:#ff9,stroke:#333
    style E fill:#ff9,stroke:#333
```

#### Detaljer per fil

##### Service-lag (kjerneendringer)

| # | Fil | Endring | Risiko |
|---|-----|---------|--------|
| 1 | `TrygdeavgiftsberegningService.kt` | `lagTrygdeavgiftsperiode()`: mapper N grunnlag fra response.grunnlag-listen til N `TrygdeavgiftsperiodeGrunnlag`-entiteter. `hentOpprinneligTrygdeavgiftsperioder()`: henter inntekt/skatteforhold via grunnlag-listen i stedet for direkte FK. | **Høy** — kjernemetode |
| 2 | `EøsPensjonistTrygdeavgiftsberegningService.kt` | Samme mønster som #1 for EØS-pensjonist-varianten. | **Høy** |
| 3 | `TrygdeavgiftperiodeErstatter.kt` | `erstattTrygdeavgiftsperioder()` linje 24-26: ID-matching via `grunnlagMedlemskapsperiode?.id` → match mot **alle** grunnlag i listen. | **Høy** — endret matchingsstrategi |
| 4 | `ReplikerBehandlingsresultatService.kt` | `copyEntity()` med grunnlag-nulling → kopier grunnlag-listen. `addGrunnlag()` → `leggTilGrunnlag()`. | **Høy** — replikering av behandlingsresultat |
| 5 | `LovvalgsperiodeService.kt` | `copyEntity()` med grunnlag-felter → kopier via ny liste. | **Middels** |
| 6 | `TrygdeavgiftMottakerService.kt` | Leser grunnlag — tilpass til liste. | **Lav** |
| 7 | `TotalbeløpBeregner.kt` | Leser grunnlag — tilpass til liste. | **Lav** |
| 8 | `ÅrsavregningIkkeSkattepliktigeFinner.kt` | Leser grunnlag — tilpass til liste. | **Lav** |

##### Brevmappere (lesing)

| # | Fil | Endring |
|---|-----|---------|
| 9 | `InnvilgelseFtrlMapper.kt` | `hentGrunnlagInntekstperiode()` → hent fra grunnlag-listen |
| 10 | `ÅrsavregningVedtakMapper.kt` | Samme mønster |
| 11 | `InformasjonTrygdeavgiftMapper.kt` | Samme mønster |

##### Frontend-API (DTO-er og controllers)

| # | Fil | Endring |
|---|-----|---------|
| 12 | `TrygdeavgiftsperiodeDto.kt` | `hentGrunnlagAvgiftsperiode().hentTrygdedekning()` — fungerer med ny hjelpemetode. Vurder: hva vises med **flere** ulike trygdedekninger? |
| 13 | `TrygdeavgiftsgrunnlagDto.kt` | Constructor tar `Set<Trygdeavgiftsperiode>`, leser `grunnlagSkatteforholdTilNorge!!` og `grunnlagInntekstperiode!!` → les fra grunnlag-listen |
| 14 | `EøsPensjonistTrygdeavgiftsperiodeDto.kt` | Tilpass til grunnlag-listen |
| 15 | `ÅrsavregningController.kt` | `mapTilTrygdeavgiftperiodeDto()` linje 219: `periode.grunnlagInntekstperiode?.kalkulertMndInntekt()` → hent fra grunnlag. **Spørsmål:** Hvilken inntektsperiode brukes for visning ved flere grunnlag? |

##### Saksflyt

| # | Fil | Endring |
|---|-----|---------|
| 16 | `OpprettFakturaserie.kt` | `hentGrunnlagInntekstperiode()` og `hentGrunnlagMedlemskapsperiode()` — tilpass |
| 17 | `BeregnOgSendFaktura.kt` | Samme mønster |

##### Integrasjon-DTO

| # | Fil | Endring |
|---|-----|---------|
| 18 | `TrygdeavgiftsgrunnlagDto.kt` (integrasjon) | `val grunnlag: TrygdeavgiftsgrunnlagDto` → `val grunnlag: List<TrygdeavgiftsgrunnlagDto>` |
| 19 | `TrygdeavgiftsberegningResponse.kt` (integrasjon) | Tilpass til liste av grunnlag |

##### Reverse relationships (JPA)

| # | Fil | Endring |
|---|-----|---------|
| 20 | `Inntektsperiode.java` | Fjern/oppdater `@OneToMany` mapping til Trygdeavgiftsperiode |
| 21 | `SkatteforholdTilNorge.java` | Fjern/oppdater `@OneToMany` mapping til Trygdeavgiftsperiode |

---

### Fase 5: Tester

```mermaid
graph LR
    subgraph "Unit-tester (~25 filer)"
        T1[TrygdeavgiftsberegningServiceTest.kt]
        T2[EøsPensjonistTrygdeavgiftsberegningServiceTest.kt]
        T3[TrygdeavgiftperiodeErstatterTest.kt]
        T4[TrygdeavgiftMottakerServiceTest.kt]
        T5[ReplikerBehandlingsresultatServiceTest.kt]
        T6[ÅrsavregningServiceTestBase.kt]
        T7[TrygdeavgiftControllerTest.kt]
        T8[ÅrsavregningControllerTest.kt]
        T9["Brevmapper-tester (4 stk)"]
        T10["Saksflyt-tester (3 stk)"]
    end

    subgraph "Test factories"
        F1[TrygdeavgiftsperiodeTestFactory.kt]
        F2[BehandlingsresultatTestFactory.kt]
        F3[MedlemskapsperiodeTestFactory.kt]
    end

    subgraph "Integrasjonstester"
        I1[YrkesaktivFtrlVedtakIT.kt]
        I2[PensjonistFtrlVedtakIT.kt]
        I3[ÅrsavregningIkkeSkattepliktigeIT.kt]
        I4[BehandlingsresultatServiceIT.kt]
        I5[LovvalgsperiodeServiceIT.kt]
    end

    F1 --> T1 & T2 & T3
    F2 --> T5 & T6
```

**Strategi:**
1. Oppdater test factories **først** — alle tester bruker disse
2. Oppdater unit-tester modul for modul
3. Kjør integrasjonstester til slutt (krever `USE-LOCAL-DB=true` på ARM Mac)

---

## 5. Åpne spørsmål som må avklares

### Faglige spørsmål

| # | Spørsmål | Kontekst | Forslag |
|---|----------|----------|---------|
| 1 | **Trygdedekning ved flere grunnlag** | `TrygdeavgiftsperiodeDto.kt` kaller `hentGrunnlagAvgiftsperiode().hentTrygdedekning()`. Hva vises når én samlet periode har grunnlag med ulik dekning? | Vis dekningen fra **første** grunnlag (den som definerer periodens karakter). Eventuelt: vis alle distinkte dekninger. Avklar med Francois/fagansvarlig. |
| 2 | **Inntektsvisning i årsavregning** | `ÅrsavregningController.kt` leser `grunnlagInntekstperiode?.kalkulertMndInntekt()`. Med flere grunnlag — summere? Vise første? | Trolig: **summere** månedsinntekt fra alle grunnlag. Avklar med frontend-oppgave [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530). |
| 3 | **Faktura-beløp per inntektskilde** | `OpprettFakturaserie.kt` bruker `hentGrunnlagInntekstperiode()` for å hente faktura-beløp. Med flere grunnlag — én fakturalinje per grunnlag eller summert? | Avklar med faktureringslogikk. |

### Faglige spørsmål (7969-spesifikke)

| # | Spørsmål | Kontekst | Forslag |
|---|----------|----------|---------|
| 4 | **Hva skal vises i brev når 25%-regelen brukes?** | Brevmappere viser i dag sats per periode. Med `sats = null` — hva står i brevet? "Beregnet etter 25%-regelen" uten sats? | Avklar med brevmal-eier. Foreslår: ny brev-seksjon som forklarer at 25%-regelen er brukt, med totalbeløp i stedet for sats. |
| 5 | **Hva med perioder der minstebeløpet slår inn?** | Minstebeløp → ingen avgift. Skal disse periodene i det hele tatt lagres som `Trygdeavgiftsperiode` med `beregningstype = MINSTEBELOEP`? | Trolig ja — for sporbarhet. Men avklar: `månedsavgift = 0` og `sats = null`? Eller filtreres de bort? |
| 6 | **Sats i frontend/årsavregning** | `ÅrsavregningController` og DTOer sender `trygdesats.toDouble()` til frontend. Med null — 0.0? Eller eget felt for beregningstype? | Frontend-oppgave [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530) bør inkludere visning av beregningstype. |

### Tekniske spørsmål

| # | Spørsmål | Kontekst | Forslag |
|---|----------|----------|---------|
| 4 | **Skal gamle FK-kolonner droppes?** | V150 migrerer data. Skal V151 droppe kolonner? | Ja — i separat migrasjon etter at all kode er oppdatert og deployet. Gir en trygg overgangsperiode. |
| 5 | **Deploy-koordinering** | `melosys-trygdeavgift-beregning` endrer JSON-kontrakt. | Gjør responsen bakoverkompatibel: ved ubegrenset beregning returneres liste med 1 element. Deploy beregning først, deretter API. |
| 6 | **`copyEntity()` semantikk** | Hva betyr det å kopiere en periode med N grunnlag? Skal grunnlagene deles eller deep-copies? | Deep-copy (nye entiteter med `id = null`). Eksisterende mønster i `ReplikerBehandlingsresultatService` gjør dette allerede for FK-ene. |
| 7 | **Nullable sats i `fattet-vedtak-schema.json`** (7969) | `service/src/main/resources/fattet-vedtak-schema.json` refererer `trygdesats`. Kafka-konsumenter kan bryte på null. | Oppdater JSON-schema til å tillate null. Sjekk om statistikk-modul (`UtstedtA1Service`) leser sats. |
| 8 | **`erLikForSatsendring()` med null** (7969) | Brukes av satsendring-logikk for å sjekke om perioder har endret seg. `null.compareTo(null)` kaster NPE. | Implementer null-safe sammenligning: `trygdesats == other.trygdesats` (Kotlin's `==` er null-safe) i stedet for `.compareTo()`. |

---

## 6. Implementeringsrekkefølge

```mermaid
gantt
    title Implementeringsfaser (7588 + 7969)
    dateFormat  YYYY-MM-DD
    axisFormat  %d.%m

    section Repo: melosys-trygdeavgift-beregning
    Ny enum Avgiftsberegningstype (7969)            :a0, 2026-03-17, 1d
    Endre Response til List + beregningstype        :a1, 2026-03-17, 1d
    Fiks .last() + sats=null i BeregningService     :a2, after a1, 1d
    Fiks EøsPensjonistBeregningService              :a3, after a1, 1d
    Oppdater tester                                 :a4, after a2, 1d
    Deploy til q1                                   :milestone, a5, after a4, 0d

    section Repo: melosys-api (fase 1 - DB)
    Ny enum Avgiftsberegningstype (7969)            :b0, after a4, 1d
    Ny entitet TrygdeavgiftsperiodeGrunnlag (7588)  :b1, after a4, 1d
    Flyway V150 - grunnlag-tabell + beregningstype + nullable sats  :b2, after b1, 1d

    section Repo: melosys-api (fase 2 - entitet)
    Oppdater Trygdeavgiftsperiode.kt (begge)        :c1, after b2, 1d
    Oppdater test factories                         :c2, after c1, 1d

    section Repo: melosys-api (fase 3 - services)
    TrygdeavgiftsberegningService (begge)           :d1, after c2, 2d
    TrygdeavgiftperiodeErstatter (7588)             :d2, after c2, 1d
    ReplikerBehandlingsresultatService (7588)        :d3, after c2, 1d
    EøsPensjonistTrygdeavgiftsberegningService       :d4, after c2, 1d
    Andre services (6 stk)                          :d5, after c2, 2d

    section Repo: melosys-api (fase 4 - DTO/API + brev)
    Integrasjon-DTO-er (begge)                      :e1, after d1, 1d
    Frontend-API DTO-er + controllers (begge)        :e2, after d1, 1d
    Brevmappere - nullable sats + beregningstype     :e3, after d1, 1d
    Saksflyt-steg - nullable sats i fakturatekst     :e4, after d1, 1d

    section Testing
    Unit-tester                                     :f1, after e4, 2d
    Integrasjonstester                              :f2, after f1, 1d
    Deploy til q1                                   :milestone, f3, after f2, 0d

    section Opprydding
    Flyway V151 - dropp gamle FK-kolonner           :g1, after f3, 1d
```

---

## 7. Risikomatrise

| Risiko | Sannsynlighet | Konsekvens | Tiltak |
|--------|---------------|------------|--------|
| Datamigrasjon feiler (NULL i inntektsperiode/skatteforhold) | Middels | Høy | Legg til WHERE-filter i INSERT; håndter perioder uten grunnlag separat |
| Deploy-rekkefølge feil — API leser nytt format fra gammel beregning | Lav | Høy | Bakoverkompatibel JSON (liste med 1 element = gammelt format) |
| `ReplikerBehandlingsresultatService` bryter | Høy | Høy | Grundig testing — denne er vanskeligst å refaktorere |
| Brevmappere viser feil grunnlag | Middels | Middels | Manuell test av brev-generering i q1 |
| JPA cascade-problemer med ny `@OneToMany` | Middels | Middels | Teste med integrasjonstester mot Oracle |
| **Nullable sats bryter NullPointerException** (7969) | **Høy** | **Høy** | 48 filer refererer `trygdesats`. Gjør systematisk gjennomgang. Bruk `erBegrenset()` som guard i stedet for å sjekke `trygdesats == 0` |
| **Fakturatekst med null sats** (7969) | Middels | Middels | `OpprettFakturaserie.kt` og `BeregnOgSendFaktura.kt` interpolerer sats i string. Test at brev/faktura ikke viser "Sats: null %" |
| **Eksisterende data uten beregningstype** (7969) | Lav | Lav | Migrasjon setter `ORDINAER` som default — alle eksisterende perioder er ordinære |

---

## 8. Akseptansekriterier

### MELOSYS-7588 (flere grunnlag)
- [ ] Ny tabell `trygdeavgiftsperiode_grunnlag` opprettet i Oracle
- [ ] Alle eksisterende data migrert fra gamle FK-kolonner til ny tabell
- [ ] `melosys-trygdeavgift-beregning` returnerer liste av grunnlag per beregnet periode
- [ ] `melosys-api` mottar og lagrer N grunnlag per trygdeavgiftsperiode
- [ ] `TrygdeavgiftperiodeErstatter` matcher korrekt med flere grunnlag
- [ ] `ReplikerBehandlingsresultatService` kopierer grunnlag-listen ved ny vurdering
- [ ] Gamle FK-kolonner droppet i separat migrasjon etter validering

### MELOSYS-7969 (beregningstype + nullable sats)
- [ ] `melosys-trygdeavgift-beregning` returnerer `beregningstype` og `sats: null` ved 25%-regel/minstebeløp
- [ ] `melosys-api` lagrer `beregningstype` (enum) på `Trygdeavgiftsperiode`
- [ ] `trygdesats`-kolonnen er nullable i Oracle og i JPA-entiteten
- [ ] Brevmappere viser "25%-regel" eller "Minstebeløp" i stedet for sats når beregningstype != ORDINAER
- [ ] Fakturatekst håndterer null sats korrekt (ingen "Sats: null %")
- [ ] Frontend viser beregningstype (koordinert med [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530))

### Felles
- [ ] Alle eksisterende tester oppdatert og passerer
- [ ] Integrasjonstester verifiserer end-to-end flyt med 25%-regel
- [ ] Brev generert korrekt for alle scenarioer (ordinær, 25%-regel, minstebeløp)

---

## 9. Relaterte oppgaver

| Jira | Tittel | Relasjon |
|------|--------|----------|
| [MELOSYS-7464](https://jira.adeo.no/browse/MELOSYS-7464) | Støtte til 25%-regelen og minstebeløpet | Epic (parent) |
| [MELOSYS-7557](https://jira.adeo.no/browse/MELOSYS-7557) | Teknisk analyse | Konkluderer med "lang" periode-tilnærming |
| [MELOSYS-7969](https://jira.adeo.no/browse/MELOSYS-7969) | Bruk av 25%-regel eller minstebeløp må lagres | **Slått sammen i denne planen** — beregningstype + nullable sats |
| [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530) | Tilpasse visning av beregning og grunnlag | Frontend — konsumerer begge endringene |
| [MELOSYS-6631](https://jira.adeo.no/browse/MELOSYS-6631) | Forenkling av datamodell: Fjern trygdeavgiftsgrunnlag | Historikk — forrige forenklingrunde |
| [MELOSYS-7158](https://jira.adeo.no/browse/MELOSYS-7158) | Feil beregning ved flere medlemskapsperioder | Symptom på 1:1-begrensningen |
| [MELOSYS-6688](https://jira.adeo.no/browse/MELOSYS-6688) | Støtte til 25%-regel i årsavregningen — MVP | Manuell overstyring (beholdes parallelt) |
