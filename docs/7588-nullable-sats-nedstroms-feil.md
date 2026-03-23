# 7588/7969: Nullable trygdesats — nedstroms feil og fikser

**Dato:** 2026-03-22 (oppdatert 2026-03-23)
**Oppdaget i:** E2E-testkjoring med `7588-grunnlag` images
**Siste CI-run:** https://github.com/navikt/melosys-e2e-tests/actions/runs/23425338486
**Resultat:** 64 passed, 4 failed, 2 flaky (forbedret fra 61 passed, 7 failed)

---

## Oversikt

MELOSYS-7588 + MELOSYS-7969 gjor `trygdesats` nullable i `Trygdeavgiftsperiode`.
Nar 25%-regelen eller minstebelopet slar inn, settes `sats = null` i stedet for
en tallverdi. Dette bryter nedstroms tjenester og brevmaler som ikke hanterer null.

## Status per tjeneste

| Tjeneste | Feil | Status | Image |
|----------|------|--------|-------|
| melosys-api | ORA-02292 FK cascade, Rounding necessary, brevmapper `!!` | FIKSET | `7588-grunnlag` |
| melosys-dokgen | Schema avviser null, Handlebars gt-krasj | FIKSET | `7588-grunnlag` |
| faktureringskomponenten | ArithmeticException Rounding necessary | FIKSET | `7588-grunnlag` |
| melosys-trygdeavgift-beregning | (opprinnelig kilde til nullable sats) | OK | `7588-grunnlag` |
| melosys-e2e-tests | Race condition nyvurderingbakgrunn | UNDER ARBEID | branch: feature/ftrl-trygdeavgift-25-prosent-regel-e2e |

---

## Feil 1: Faktureringskomponenten — ArithmeticException (FIKSET)

**Arsak:** BelopBeregner.kt brukte `RoundingMode.UNNECESSARY` i `setScale(2)`. Nar enhetspris
med desimaler (f.eks. 23548.92) multipliseres med antall maneder, kan resultatet ha mer enn
2 desimaler, som kaster ArithmeticException.

**Fiks:** Endret til `RoundingMode.HALF_UP`. Commit: `0c3704d`, image: `faktureringskomponenten:7588-grunnlag`.

## Feil 2: Dokgen — Schema-validering + Handlebars-krasj (FIKSET)

Feilen hadde to lag:

### a) JSON-schema avviste null avgiftssats
6 brevmal-schemaer hadde `"avgiftssats": { "type": "number" }` som required-felt.

**Fiks:** Endret til `"type": ["number", "null"]` og fjernet `avgiftssats` fra required i:
- innvilgelse_ftrl, pliktig_medlem_ftrl, pensjonist_pliktig_ftrl
- pensjonist_frivillig_ftrl, aarsavregning_vedtaksbrev, trygdeavgift_informasjonsbrev

### b) Handlebars `gt`-helper krasjet pa null
6 beregningstabell.hbs-filer brukte `(gt periode.avgiftssats 0)` som kaster
`IllegalArgumentException: Not a comparable: null` nar avgiftssats er null.

**Fiks:**
- `(gt periode.avgiftssats 0)` erstattet med `(gt periode.avgiftPerMd 0)` (belop er aldri null)
- Sats-visning: `{{periode.avgiftssats}} %` erstattet med `{{#if periode.avgiftssats}}{{periode.avgiftssats}} %{{else}}**{{/if}}`
- FtrlHelper `har_periode_med_inntektskildetype_og_sats`: sjekker `avgiftPerMd` i stedet for `avgiftssats`, null-safe

## Feil 3: melosys-api — ORA-02292 FK_TAG_MEDLEMSKAPSPERIODE (FIKSET)

**Arsak:** V151 opprettet `trygdeavgiftsperiode_grunnlag` med FK til `medlemskapsperiode`,
`lovvalgsperiode` og `helseutgift_dekkes_periode` uten cascade. Nar Hibernate slettet
en Medlemskapsperiode (ved nyvurdering), blokkerte FK-en med ORA-02292.

**Fiks:** V152 utvidet med `ON DELETE SET NULL` pa de tre avgiftspliktig-periode FK-ene.
Hibernate kan slette i vilkarlig rekkefolge; grunnlag-raden ryddes opp etterpaa via
`ON DELETE CASCADE` pa `trygdeavgiftsperiode_id`.

## Feil 4: melosys-api — Rounding necessary i ArsavregningController (FIKSET)

**Arsak:** `ArsavregningController.mapTilTrygdeavgiftperiodeDto()` brukte
`trygdeavgiftsbelopMd.hentVerdi().intValueExact()`. 25%-regelen kan gi belop med
desimaler (f.eks. 3448.33) som `intValueExact()` ikke hanterer.

**Fiks:** Endret til `.setScale(0, RoundingMode.HALF_UP).intValueExact()`.

## Feil 5: melosys-api — InformasjonTrygdeavgiftMapper brukte `!!` (FIKSET)

**Arsak:** `InformasjonTrygdeavgiftMapper.mapAvgiftsperioderPensjonist()` brukte
`grunnlagInntekstperiode!!` og `grunnlagSkatteforholdTilNorge!!` direkte pa legacy FK-felter
som kan vaere null nar data leses fra ny grunnlag-tabell.

**Fiks:** Byttet til `hentGrunnlagInntekstperiode()` / `hentGrunnlagSkatteforholdTilNorge()`
som faller tilbake korrekt.

## Feil 6: Fakturaserie-referanse mangler for nyvurdering (UNDER ANALYSE)

**Symptom:** 4 E2E-tester feiler med:
```
Failed to get fakturaserie null: 404
  /fakturaserier/null → "Fant ikke fakturaserie pa: null"
```
`getFakturaserieReferanse(behandlingId)` returnerer null for nyvurdering-behandlingen.

**Feilende tester:**
- komplett-sak-flere-land-arbeidsinntekt-nv-kansellering
- komplett-sak-flere-land-arbeidsinntekt
- komplett-sak-nv-annulering-lukker-apne-arsavregninger
- komplett-sak-nv-periode-endres-til-kun-tidligere-ar

**Mulig arsak:** OPPRETT_FAKTURASERIE-steget kjorer for nyvurdering-behandlingen, men
loggen viser "Ingen fakturaserie opprettet". Enten:
- `harFakturerbarTrygdeavgift()` returnerer false (trygdeavgiftsperiodene mangler avgift)
- `skalFaktureres()` returnerer false (betalingstype feil)
- Fakturaserie-referansen lagres ikke tilbake i behandling-tabellen

**Ikke relatert til harAvgift()-endringen** — den nye sjekken er MINDRE restriktiv
(kun belop-basert, ikke sats+belop), sa flere perioder vil kvalifisere.

**Neste steg:** Legg til debug-logging i OpprettFakturaserie for a identifisere
hvilken betingelse som feiler. Verifiser om dette er pre-eksisterende.

---

## Deploy-rekkefolge

Alle 4 tjenester kan deployes uavhengig takket vaere bakoverkompatibel strategi:

1. **faktureringskomponenten** — RoundingMode-fiks (uavhengig PR)
2. **melosys-dokgen** — Schema + template-fiks (uavhengig PR)
3. **melosys-api** — V152 cascade + brevmapper + intValueExact (del av 7588-grunnlag PR #3273)
4. **melosys-trygdeavgift-beregning** — Allerede deployklar (PR #379)

## Referanser

- [MELOSYS-7588](https://jira.adeo.no/browse/MELOSYS-7588) — Utvid datamodell for grunnlag
- [MELOSYS-7969](https://jira.adeo.no/browse/MELOSYS-7969) — Lagre beregningstype, nullable sats
- [PR #3273 melosys-api](https://github.com/navikt/melosys-api/pull/3273)
- [PR #379 melosys-trygdeavgift-beregning](https://github.com/navikt/melosys-trygdeavgift-beregning/pull/379)
- [Implementeringsplan](MELOSYS-7588-implementeringsplan.md)
- [ORA-01407 fix](ORA-01407-trygdeavgiftsperiode-grunnlag.md)
