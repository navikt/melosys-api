# MELOSYS-7588: Utvid datamodell for trygdeavgiftsperioder til å støtte flere grunnlagsperioder

**Dato:** 2026-03-16
**Epic:** [MELOSYS-7464](https://jira.adeo.no/browse/MELOSYS-7464) — Støtte til 25%-regelen og minstebeløpet
**Teknisk analyse:** [MELOSYS-7557](https://jira.adeo.no/browse/MELOSYS-7557)
**Confluence:** [Eksempler på fastsettelse av trygdeavgift](https://confluence.adeo.no/spaces/TEESSI/pages/535938349) | [25%-regelen](https://confluence.adeo.no/spaces/TEESSI/pages/704156896) | [Fysisk DB-modell](https://confluence.adeo.no/spaces/TEESSI/pages/603722350)

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
        BigDecimal trygdesats
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

    Note over BEREGN: I DAG: grunnlag = AvgiftsperiodeGrunnlag(perioder.last())
    Note over BEREGN: NYTT: grunnlag = List<AvgiftsperiodeGrunnlag>(alle perioder)

    BEREGN-->>API: List<BeregnetTrygdeavgiftResponse>

    Note over API: Mottar N grunnlag per respons-element
    API->>API: lagTrygdeavgiftsperiode(): mapper alle grunnlag til TrygdeavgiftsperiodeGrunnlag-entiteter
    API->>API: Lagrer via JPA cascade
```

**Deploy-rekkefølge:** `melosys-trygdeavgift-beregning` MÅ deployes **før** eller **samtidig med** `melosys-api`, fordi API-et må kunne lese den nye JSON-strukturen. Alternativt: gjør endringen bakoverkompatibel med en overgangsperiode.

---

## 4. Detaljert endringsplan

### Fase 1: melosys-trygdeavgift-beregning — API-kontraktendring

#### Filer som endres

| Fil | Endring |
|-----|---------|
| `standard/modell/AvgiftsperiodeGrunnlag.kt` | Uendret (brukes fortsatt for enkeltelement) |
| `standard/modell/BeregnetTrygdeavgiftResponse.kt` | `grunnlag: AvgiftsperiodeGrunnlag` → `grunnlag: List<AvgiftsperiodeGrunnlag>` |
| `standard/BeregningService.kt` | 3 steder: `.last()` → pass hele listen |
| `eospensjonist/modell/EøsPensjonistBeregnetTrygdeavgiftResponse.kt` | `grunnlag: EøsPensjonistAvgiftsperiodeGrunnlag` → `grunnlag: List<...>` |
| `eospensjonist/EøsPensjonistBeregningService.kt` | 1 sted: `.last()` → pass hele listen |
| Berørte tester | Alle tester som konstruerer `BeregnetTrygdeavgiftResponse` |

#### Detalj: .last()-stedene som fikses

**1. `opprettBegrensetAvgiftResponse` (pliktig, linje 374)**
```kotlin
// FØR:
AvgiftsperiodeGrunnlag(grunnlagPerioder.last())
// ETTER:
grunnlagPerioder.map { AvgiftsperiodeGrunnlag(it) }
```

**2. `opprettBegrensetResponse` (frivillig helse/pensjon, linje 275)**
```kotlin
// FØR:
grunnlagPeriode = avgiftsgrunnlagPerioder.last()
// ETTER:
grunnlagPerioder = avgiftsgrunnlagPerioder  // hele listen
```

**3. `opprettMisjonærBegrensetAvgiftResponse` (misjonær, linje 145)**
```kotlin
// FØR:
grunnlagPeriode = periodeberegninger.last().grunnlagPeriode
// ETTER:
grunnlagPerioder = periodeberegninger.map { it.grunnlagPeriode }
```

**4. `EøsPensjonistBeregningService.opprettBegrensetAvgiftResponse` (linje 95)**
```kotlin
// Samme mønster — .last() → .map { ... }
```

#### Ny JSON-kontrakt (respons)

```json
{
  "beregnetPeriode": {
    "periode": { "fom": "2025-05-01", "tom": "2025-12-31" },
    "sats": 0.00,
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
  ]
}
```

**Bakoverkompatibilitet:** Ubegrenset perioder (uten 25%-regel) returnerer en liste med **ett** element, så melosys-api kan håndtere begge tilfeller uten å kreve ny versjon.

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

#### Flyway-migrasjon: `V150__trygdeavgiftsperiode_grunnlag.sql`

```sql
-- 1. Ny tabell
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

-- 2. Migrer eksisterende data
INSERT INTO trygdeavgiftsperiode_grunnlag
    (trygdeavgiftsperiode_id, medlemskapsperiode_id, lovvalgsperiode_id,
     helseutgift_dekkes_periode_id, inntektsperiode_id, skatteforhold_id)
SELECT
    t.id, t.medlemskapsperiode_id, t.lovvalg_periode_id,
    t.helseutgift_dekkes_periode_id, t.inntektsperiode_id, t.skatteforhold_id
FROM trygdeavgiftsperiode t
WHERE t.inntektsperiode_id IS NOT NULL
  AND t.skatteforhold_id IS NOT NULL;

-- 3. Dropp gamle FK-kolonner (etter validering at alt er migrert)
-- VIKTIG: Gjøres i separat migrasjon V151 etter at koden er oppdatert
```

**Separat migrasjon `V151__fjern_gamle_grunnlag_fk.sql`** (etter at all kode er oppdatert):
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

**Fjernes:**
- De 5 FK-feltene (`grunnlagMedlemskapsperiode`, `grunnlagLovvalgsPeriode`, `grunnlagHelseutgiftDekkesPeriode`, `grunnlagInntekstperiode`, `grunnlagSkatteforholdTilNorge`)
- `addGrunnlag()`-metoden
- Getter-metodene `hentGrunnlagMedlemskapsperiode()`, `hentGrunnlagInntekstperiode()`, `hentGrunnlagSkatteforholdTilNorge()`

**Legges til:**
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
```

**Oppdateres:**
- `copyEntity()` — kopierer grunnlag-listen
- `erLikForSatsendring()` — sammenligner grunnlag-lister

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

### Tekniske spørsmål

| # | Spørsmål | Kontekst | Forslag |
|---|----------|----------|---------|
| 4 | **Skal gamle FK-kolonner droppes?** | V150 migrerer data. Skal V151 droppe kolonner? | Ja — i separat migrasjon etter at all kode er oppdatert og deployet. Gir en trygg overgangsperiode. |
| 5 | **Deploy-koordinering** | `melosys-trygdeavgift-beregning` endrer JSON-kontrakt. | Gjør responsen bakoverkompatibel: ved ubegrenset beregning returneres liste med 1 element. Deploy beregning først, deretter API. |
| 6 | **`copyEntity()` semantikk** | Hva betyr det å kopiere en periode med N grunnlag? Skal grunnlagene deles eller deep-copies? | Deep-copy (nye entiteter med `id = null`). Eksisterende mønster i `ReplikerBehandlingsresultatService` gjør dette allerede for FK-ene. |

---

## 6. Implementeringsrekkefølge

```mermaid
gantt
    title Implementeringsfaser
    dateFormat  YYYY-MM-DD
    axisFormat  %d.%m

    section Repo: melosys-trygdeavgift-beregning
    Endre BeregnetTrygdeavgiftResponse til List    :a1, 2026-03-17, 1d
    Fiks .last() i BeregningService                :a2, after a1, 1d
    Fiks .last() i EøsPensjonistBeregningService   :a3, after a1, 1d
    Oppdater tester                                :a4, after a2, 1d
    Deploy til q1                                  :milestone, a5, after a4, 0d

    section Repo: melosys-api (fase 1 - DB)
    Ny entitet TrygdeavgiftsperiodeGrunnlag        :b1, after a4, 1d
    Flyway V150 - ny tabell + datamigrasjon        :b2, after b1, 1d

    section Repo: melosys-api (fase 2 - entitet)
    Oppdater Trygdeavgiftsperiode.kt               :c1, after b2, 1d
    Oppdater test factories                        :c2, after c1, 1d

    section Repo: melosys-api (fase 3 - services)
    TrygdeavgiftsberegningService                  :d1, after c2, 2d
    TrygdeavgiftperiodeErstatter                   :d2, after c2, 1d
    ReplikerBehandlingsresultatService             :d3, after c2, 1d
    EøsPensjonistTrygdeavgiftsberegningService     :d4, after c2, 1d
    Andre services (6 stk)                         :d5, after c2, 2d

    section Repo: melosys-api (fase 4 - DTO/API)
    Integrasjon-DTO-er                             :e1, after d1, 1d
    Frontend-API DTO-er og controllers             :e2, after d1, 1d
    Brevmappere                                    :e3, after d1, 1d
    Saksflyt-steg                                  :e4, after d1, 1d

    section Testing
    Unit-tester                                    :f1, after e4, 2d
    Integrasjonstester                             :f2, after f1, 1d
    Deploy til q1                                  :milestone, f3, after f2, 0d

    section Opprydding
    Flyway V151 - dropp gamle FK-kolonner          :g1, after f3, 1d
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

---

## 8. Akseptansekriterier

- [ ] Ny tabell `trygdeavgiftsperiode_grunnlag` opprettet i Oracle
- [ ] Alle eksisterende data migrert fra gamle FK-kolonner til ny tabell
- [ ] `melosys-trygdeavgift-beregning` returnerer liste av grunnlag per beregnet periode
- [ ] `melosys-api` mottar og lagrer N grunnlag per trygdeavgiftsperiode
- [ ] `TrygdeavgiftperiodeErstatter` matcher korrekt med flere grunnlag
- [ ] `ReplikerBehandlingsresultatService` kopierer grunnlag-listen ved ny vurdering
- [ ] Brevmappere genererer korrekte brev med flere grunnlag
- [ ] Frontend viser korrekt informasjon (koordinert med [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530))
- [ ] Alle eksisterende tester oppdatert og passerer
- [ ] Integrasjonstester verifiserer end-to-end flyt med 25%-regel
- [ ] Gamle FK-kolonner droppet i separat migrasjon etter validering

---

## 9. Relaterte oppgaver

| Jira | Tittel | Relasjon |
|------|--------|----------|
| [MELOSYS-7464](https://jira.adeo.no/browse/MELOSYS-7464) | Støtte til 25%-regelen og minstebeløpet | Epic (parent) |
| [MELOSYS-7557](https://jira.adeo.no/browse/MELOSYS-7557) | Teknisk analyse | Konkluderer med "lang" periode-tilnærming |
| [MELOSYS-7530](https://jira.adeo.no/browse/MELOSYS-7530) | Tilpasse visning av beregning og grunnlag | Frontend — konsumerer denne endringen |
| [MELOSYS-6631](https://jira.adeo.no/browse/MELOSYS-6631) | Forenkling av datamodell: Fjern trygdeavgiftsgrunnlag | Historikk — forrige forenklingrunde |
| [MELOSYS-7158](https://jira.adeo.no/browse/MELOSYS-7158) | Feil beregning ved flere medlemskapsperioder | Symptom på 1:1-begrensningen |
