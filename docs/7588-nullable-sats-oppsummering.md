# MELOSYS-7588/7969: Nullable trygdesats — oppsummering av feil og fikser

**Dato:** 2026-03-22 til 2026-03-23
**Oppgaver:** MELOSYS-7588 (1:N grunnlag) + MELOSYS-7969 (beregningstype, nullable sats)

---

## Bakgrunn

MELOSYS-7588 og MELOSYS-7969 endrer datamodellen for trygdeavgiftsperioder:

- **Nullable sats:** Nar 25%-regelen eller minstebelopet slar inn, er det ingen sats — bare et totalbelop. `trygdesats` endres fra `NOT NULL` til nullable.
- **1:N grunnlag:** En trygdeavgiftsperiode kan na referere flere grunnlag (inntektsperioder/skatteforhold) via ny `trygdeavgiftsperiode_grunnlag`-tabell.
- **Beregningstype:** Ny enum `Avgiftsberegningstype` (ORDINAER, TJUEFEM_PROSENT_REGEL, MINSTEBELOEP).

Endringene berorer 5 repoer. E2E-testing avdekket 7 feil pa tvers av hele stacken.

---

## Feil og fikser per repo

### 1. melosys-api (PR #3273)

#### a) ORA-02292: FK_TAG_MEDLEMSKAPSPERIODE violated

**Problem:** Ny `trygdeavgiftsperiode_grunnlag`-tabell (V151) hadde FK til `medlemskapsperiode`, `lovvalgsperiode` og `helseutgift_dekkes_periode` uten cascade-regler. Nar Hibernate slettet en `Medlemskapsperiode` ved nyvurdering, blokkerte FK-en fordi grunnlag-raden fortsatt refererte den.

**Fiks:** V152 utvidet med `ON DELETE SET NULL` pa de tre avgiftspliktig-periode FK-ene. Hibernate kan na slette i vilkarlig rekkefolge. Grunnlag-raden ryddes opp etterpaa via `ON DELETE CASCADE` pa `trygdeavgiftsperiode_id`.

**Fil:** `V152__trygdeavgiftsperiode_grunnlag_on_delete_cascade.sql`

#### b) ArithmeticException: Rounding necessary i ArsavregningController

**Problem:** `ArsavregningController.mapTilTrygdeavgiftperiodeDto()` brukte `intValueExact()` pa `trygdeavgiftsbelopMd`. 25%-regelen kan gi belop med desimaler (f.eks. 3448.33), og `intValueExact()` kaster exception nar verdien ikke er et eksakt heltall.

**Fiks:** Endret til `.setScale(0, RoundingMode.HALF_UP).intValueExact()` — avrunder til narmeste heltall for konvertering.

**Fil:** `ArsavregningController.kt`

#### c) InformasjonTrygdeavgiftMapper brukte `!!` pa legacy FK-er

**Problem:** `InformasjonTrygdeavgiftMapper.mapAvgiftsperioderPensjonist()` brukte `grunnlagInntekstperiode!!` og `grunnlagSkatteforholdTilNorge!!` direkte. Med ny grunnlag-tabell kan legacy FK-feltene vaere null.

**Fiks:** Byttet til `hentGrunnlagInntekstperiode()` og `hentGrunnlagSkatteforholdTilNorge()` som faller tilbake korrekt mellom ny tabell og legacy FK-er.

**Fil:** `InformasjonTrygdeavgiftMapper.kt`

---

### 2. melosys-dokgen

#### a) JSON-schema avviste null avgiftssats

**Problem:** 6 brevmal-schemaer definerte `avgiftssats` som `"type": "number"` og required. Nar melosys-api sendte `null` for sats ved 25%-regelen, avviste dokgen med `"expected type: Number, found: Null"`.

**Fiks:** Endret alle 6 schemaer:
- `"type": "number"` -> `"type": ["number", "null"]`
- Fjernet `avgiftssats` fra required-arrayet

**Filer:** `schema.json` i innvilgelse_ftrl, pliktig_medlem_ftrl, pensjonist_pliktig_ftrl, pensjonist_frivillig_ftrl, aarsavregning_vedtaksbrev, trygdeavgift_informasjonsbrev

#### b) Handlebars `gt`-helper krasjet pa null

**Problem:** 6 `beregningstabell.hbs`-filer brukte `(gt periode.avgiftssats 0)` for a filtrere perioder. Handlebars' `gt`-helper kaster `IllegalArgumentException: Not a comparable: null` nar verdien er null.

**Fiks:**
- Betingelse: `(gt periode.avgiftssats 0)` -> `(gt periode.avgiftPerMd 0)` (belop er aldri null)
- Visning: `{{periode.avgiftssats}} %` -> `{{#if periode.avgiftssats}}{{periode.avgiftssats}} %{{else}}**{{/if}}`
- FtrlHelper `har_periode_med_inntektskildetype_og_sats`: sjekker `avgiftPerMd` i stedet for `avgiftssats`, med null-safe oppslag

**Filer:** 6x `beregningstabell.hbs`, `FtrlHelper.kt`, `FtrlHelperTest.kt`

---

### 3. faktureringskomponenten

**Problem:** `BelopBeregner.kt` brukte `RoundingMode.UNNECESSARY` i `setScale(2)`. Nar enhetspris med desimaler (f.eks. 23548.92) multipliseres med antall maneder, kan resultatet ha mer enn 2 desimaler, og `UNNECESSARY` kaster `ArithmeticException`.

**Fiks:** Endret til `RoundingMode.HALF_UP`.

**Fil:** `BelopBeregner.kt` (commit `0c3704d`)

---

### 4. melosys-trygdeavgift-beregning (PR #379)

Kilde til nullable sats — endringene her var planlagte og korrekte:
- `sats = null` for 25%-regel og minstebelop (i stedet for `BigDecimal.ZERO`)
- `grunnlagListe` med alle underliggende grunnlag (i stedet for `.last()`)
- `beregningstype` enum pa response

Ingen bugfikser nodvendig i dette repoet.

---

### 5. melosys-e2e-tests

#### a) Inntekt under minstebelopet

**Problem:** 4 E2E-tester brukte bruttoinntekt 10000 kr/mnd. Med 25%-regelen gir dette 60000 kr/halvar, som er under minstebelopet (99650 kr/ar). Beregningen returnerer korrekt belop=0, men testene forventet positiv avgift og fakturaserie.

**Fiks:** Okt bruttoinntekt til 100000 kr/mnd.

#### b) Feil fakturaserie API-endepunkt

**Problem:** Testene brukte `GET /fakturaserier/{referanse}` som ikke traverserer erstattet_med-kjeden (krediterings-fakturaserier).

**Fiks:** Byttet til `GET /fakturaserier?referanse=` som traverserer hele kjeden.

#### c) Dokgen-image ikke plukket opp i CI

**Problem:** Workflow-filen hadde hardkodet `:latest` for melosys-dokgen. Test-reporter manglet ogsa `MELOSYS_DOKGEN_TAG`.

**Fiks:** Gjort konfigurerbar via environment-parameter (PR #243) og lagt til i test-reporter.

---

## Tidslinje

| Tidspunkt | Hendelse | Resultat |
|-----------|----------|----------|
| 22.03 08:44 | Forste E2E-kjoring | 61 passed, 7 failed |
| 22.03 13:22 | Ny kjoring (etter V152 + InformasjonTrygdeavgiftMapper) | 62 passed, 7 failed |
| 22.03 15:22 | Dokgen schema-fiks (ble ikke brukt — image ikke plukket opp) | 62 passed, 7 failed |
| 22.03 17:30 | Dokgen template-fiks + no-cache rebuild | 64 passed, 5 failed |
| 23.03 07:00 | Alle 5 images pa 7588-grunnlag (inkl. faktureringskomponenten) | 64 passed, 4 failed |
| 23.03 10:52 | E2E test-fikser (inntekt + API-endepunkt) | Venter pa CI |

---

## Lardommer

1. **Nullable felter sprer seg:** En nullable-endring i en entitet (`trygdesats`) pavirket 6 schema-filer, 6 template-filer, 3 mapper-klasser, 1 controller og 2 eksterne tjenester. Systematisk gjennomgang er nodvendig.

2. **`intValueExact()` og `RoundingMode.UNNECESSARY` er bomber:** Begge kaster exceptions nar verdier har desimaler. Bruk alltid eksplisitt `RoundingMode` ved BigDecimal-konvertering.

3. **Handlebars-templates er usynlig kode:** Template-feil dukker ikke opp i kompilering eller unit-tester. Handlebars `gt`-helper krasjer pa null uten god feilmelding.

4. **ON DELETE CASCADE/SET NULL er nodvendig med nye FK-er:** Hibernate styrer ikke sletterekkefolge mellom entiteter. Nye FK-er i mellomtabeller ma ha cascade-regler som matcher JPA-modellen.

5. **E2E-testdata ma oppdateres ved regelendringer:** 25%-regelen endret minstebelopet fra irrelevant til avgjorende for testene. Testdata som "alltid fungerte" kan plutselig gi belop=0.

6. **Docker layer cache kan gi stale images:** Forste dokgen-push ga gammel kode pga cache. `--no-cache` var nodvendig for a fa riktig innhold.
