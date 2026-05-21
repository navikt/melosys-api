# 1. Tekstblokker og brevmaler – arkitektur

Dato: 2026-05-21

## Status

Akseptert

## Kontekst

Saksbehandlere i Melosys bruker fritekstfelter i Send brev for å skrive utfyllende
begrunnelser. Mye av denne teksten er gjenbrukbar: standardparagrafer om klagerett,
EØS-begrunnelser, komplette brevmaler. Tidligere var dette løst ved at saksbehandlere
limte inn fra eksterne Word-dokumenter, med tilhørende risiko for inkonsistens og
gammel formulering.

Vi ønsker en intern, redigerbar samling av tekstblokker og brevmaler som er:
- delt på tvers av alle saksbehandlere
- vedlikeholdbar fra et webgrensesnitt (ikke via deploy)
- søkbar fra Send brev
- versjonshåndtert via audit-logg

## Beslutning

### Én tabell med type-felt, validert i appen
Tekstblokker og brevmaler lagres i samme tabell `TEKSTBLOKK` med kolonnen `type`
(`TEKSTBLOKK` | `BREVMAL`). Vi setter **ikke** en `CHECK`-constraint i Oracle, men
validerer mot `TekstblokkType`-enumet i Java. Domenelogikken eier semantikken;
databasen lagrer rå tekst.

### Tags som join-tabell
`TEKSTBLOKK_TAG (tekstblokk_id, tag)` lagrer tags i en separat tabell med `ON DELETE
CASCADE`. Dette gir rene SQL-spørringer for tag-aggregering og åpner for fremtidig
autocomplete eller tag-baserte rapporter. Alternativ med JSON-kolonne ble forkastet
fordi Oracle JSON-funksjoner ikke er nødvendig kompleksitet for et lite domene.

### HTML-sanitering med Jsoup
All HTML saniteres i `HtmlSanitizer` (Jsoup `Safelist`) før lagring. Tillatt sett av
tagger speiler det Quill-editoren produserer (`p`, `br`, `strong`, `em`, `u`, `h2`,
`ul`, `ol`, `li`, `span`, `table`, `thead`, `tbody`, `tr`, `th`, `td`). `script`,
`style`, og alle event-attributter strippes. Sanitering på server-siden er ikke-
omgåelig — frontend-only sanitering ville vært trivielt å omgå via curl.

### Lett liste-DTO, full DTO på detalj
`GET /tekstblokker` returnerer `TekstblokkOversiktDto` uten innholdet. For 210
forventede oppføringer er hele payload ~30–60 KB. `GET /tekstblokker/{id}` returnerer
`TekstblokkDto` med full HTML. Det betyr at admin-listen og Send brev-popoveren
laster lett, mens redigering og innsetting henter detalj on-demand.

### Feature toggle gater hele controlleren
`melosys.tekstblokker` (Unleash) sjekkes ved inngangen til alle endepunkter. Hvis
av: `SikkerhetsbegrensningException` → HTTP 403 (defence-in-depth, selv om frontend
også gater menyen). Vi sjekker toggle på controller-nivå i stedet for å la service
være toggle-uvitende, slik at gate-logikk og endepunkts-eksponering er på samme sted.

### Global eierskap
Alle saksbehandlere ser alle blokker og kan redigere/slette. `registrertAv` og
`endretAv` spores via `RegistreringsInfo` (Spring Data Auditing) for revisjon.
Lavfriksjon for delt forvaltning av maler; ulykker kan gjenopprettes via audit-data.
Hvis dette viser seg å være for åpent, kan vi senere innføre eierskaps-policy uten
skjema-endring.

## Konsekvenser

**Positive**
- Saksbehandlere kan endre maler uten deploy
- Sanitering på server beskytter mot lagret HTML-injeksjon
- Type-validering i appen gir bedre feilmeldinger enn DB-constraint
- Toggle-gating sentralisert i én `sjekkToggle()`-metode

**Avveininger**
- Tag-join-tabellen krever et JOIN ved aggregering (akseptabelt med <60 unike tags)
- Sanitering kan strippe legitime tagger hvis Quill-konfig utvides; safelist må
  oppdateres parallelt
- Toggle av i prod blokkerer all CRUD — vi må aktivere toggle før vi forventer at
  saksbehandlere bruker featuren

## Filer

- Migrasjon: `app/src/main/resources/db/migration/melosysDB/V155__tekstblokker.sql`
- Entity: `domain/src/main/java/no/nav/melosys/domain/tekstblokk/`
- Repository: `repository/src/main/java/no/nav/melosys/repository/tekstblokk/`
- Service + sanitizer: `service/src/main/java/no/nav/melosys/service/tekstblokk/`
- Controller + DTOer: `frontend-api/src/main/java/no/nav/melosys/tjenester/gui/`
- Toggle: `config/src/main/kotlin/no/nav/melosys/featuretoggle/ToggleName.kt`
