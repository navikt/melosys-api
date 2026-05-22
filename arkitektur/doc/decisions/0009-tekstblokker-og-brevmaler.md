# 9. Tekstblokker og brevmaler

Date: 2026-05-22

## Status

Accepted

## Context

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

## Decision

### Én tabell med type-felt, validert i appen
Tekstblokker og brevmaler lagres i samme tabell `TEKSTBLOKK` med kolonnen `type`
(`TEKSTBLOKK` | `BREVMAL`). Vi setter **ikke** en `CHECK`-constraint i Oracle, men
validerer mot `TekstblokkType`-enumet i Kotlin. Domenelogikken eier semantikken;
databasen lagrer rå tekst.

### Tags som join-tabell
`TEKSTBLOKK_TAG (tekstblokk_id, tag)` lagrer tags i en separat tabell med
`ON DELETE CASCADE`. Dette gir rene SQL-spørringer for tag-aggregering og åpner
for fremtidig autocomplete eller tag-baserte rapporter. Alternativ med JSON-kolonne
ble forkastet fordi Oracle JSON-funksjoner ikke er nødvendig kompleksitet for et
lite domene.

### HTML-sanitering med Jsoup
All HTML saniteres i `TekstblokkHtmlSanitizer` (Jsoup `Safelist`) før lagring.
Tillatt sett av tagger speiler det Quill-editoren produserer (`p`, `br`, `strong`,
`em`, `u`, `h2`, `ul`, `ol`, `li`, `span`, `table`, `thead`, `tbody`, `tr`, `th`,
`td`). `script`, `style`, og alle event-attributter strippes. Sanitering på
server-siden er ikke-omgåelig — frontend-only sanitering ville vært trivielt å
omgå via curl.

### Lett liste-DTO via JPQL-projeksjon
`GET /tekstblokker` returnerer `TekstblokkOversiktDto` uten innhold. Repository
bruker JPQL constructor-projeksjon (`finnOversikt`) som hopper over `@Lob innhold`
fra DB-spørringen, og en separat `finnTagsForIds` slår sammen tags. For 200 rader
sparer det ~750 KB DB-IO ved hver liste-fetch. `GET /tekstblokker/{id}` returnerer
full `TekstblokkDto` med innhold; `@EntityGraph(attributePaths = "tags")` unngår
N+1 på tags-collection.

### To feature toggles: lese vs administrasjon
- `melosys.tekstblokker` (lese): styrer GET-endepunktene. 404 hvis av (endepunktet
  finnes ikke for denne brukeren), så Send brev-popoveren skjules.
- `melosys.administrasjon` (admin): styrer POST/PUT/DELETE. 403 hvis av (brukeren
  kan lese, men ikke administrere), så admin-siden er bare tilgjengelig for et
  utvalg saksbehandlere.

Gatingen er sentralisert i `sjekkLesetilgang()` og `sjekkAdministrasjon()`-helpers
på controller-laget.

### Admin bulk-import via egen controller
`TekstblokkAdminController` på `/admin/tekstblokker/bulk` er `@Unprotected` og
gates av `ApiKeyInterceptor` (X-MELOSYS-ADMIN-APIKEY) i stedet for JWT — slik at
melosys-console kan seede inn mange tekstblokker uten saksbehandler-token. Atomisk
batch (én transaksjon), `@Size(max = 500)` som DoS-cap.

### Global eierskap
Alle saksbehandlere med `melosys.administrasjon` ser og kan endre alle blokker.
`registrertAv` og `endretAv` spores via `RegistreringsInfo` (Spring Data Auditing)
for revisjon. Lavfriksjon for delt forvaltning; ulykker kan gjenopprettes via
audit-data. Hvis dette viser seg å være for åpent, kan vi senere innføre
eierskaps-policy uten skjema-endring.

### Kotlin
Hele tekstblokk-modulen er skrevet i Kotlin (domain, repository, service,
frontend-api), i tråd med ADR-0002.

## Consequences

**Positive**
- Saksbehandlere kan endre maler uten deploy
- Sanitering på server beskytter mot lagret HTML-injeksjon
- Type-validering i appen gir bedre feilmeldinger enn DB-constraint
- To-toggle-modellen lar oss rulle ut lese-funksjonalitet bredt mens admin
  begrenses til et utvalg
- Bulk-endepunkt gir effektiv seeding fra melosys-console
- Lett liste-DTO sparer ~750 KB DB-IO per liste-fetch

**Avveininger**
- Tag-join-tabellen krever et JOIN ved aggregering (akseptabelt med <60 unike tags)
- Sanitering kan strippe legitime tagger hvis Quill-konfig utvides; safelist må
  oppdateres parallelt med `htmlEditor.tsx` i melosys-web
- Lese-toggle av i prod blokkerer både popoveren og admin-siden, så toggle må
  aktiveres før saksbehandlere kan ta i bruk featuren
- `TekstblokkOversikt` har mutbar `var tags` for å støtte to-stegs JPQL+tag-join
  pattern; vurdert akseptabelt for å holde projeksjonen idiomatisk

## Filer

- Migrasjon: `app/src/main/resources/db/migration/melosysDB/V155__tekstblokker.sql`
- Entity: `domain/src/main/kotlin/no/nav/melosys/domain/tekstblokk/`
- Repository: `repository/src/main/kotlin/no/nav/melosys/repository/tekstblokk/`
- Service + sanitizer: `service/src/main/kotlin/no/nav/melosys/service/tekstblokk/`
- Controllers + DTOer: `frontend-api/src/main/kotlin/no/nav/melosys/tjenester/gui/`
- Toggles: `config/src/main/kotlin/no/nav/melosys/featuretoggle/ToggleName.kt`
- Exception-handler: `MethodArgumentNotValidException` → 400 i
  `frontend-api/.../unntakshandtering/ExceptionMapper.kt`
