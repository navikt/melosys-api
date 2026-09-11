# Statistikk — rammeavtale om fjernarbeid (TWFA)

Uttrekk til Medlemskap og avgift over saker behandlet etter rammeavtalen for fjernarbeid i EØS
(Telework Framework Agreement). Erstatter en manuell Excel-rutine. Se MELOSYS-8150.

## Hvor flagget kommer fra

Saksbehandlerens avhuking for «rammeavtale om fjernarbeid» lagres på kolonnen
`anmodningsperiode.er_fjernarbeid_twfa` (`V170__anmodningsperiode_fjernarbeid_twfa.sql`, backfill av
historikken i `V171__backfill_anmodningsperiode_fjernarbeid_twfa.sql`):

```
melosys-web (checkbox)
  → AnmodningUnntakDto.erFjernarbeidTWFA
  → AnmodningUnntakService.anmodningOmUnntak
  → AnmodningsperiodeService.registrerAnmodning   (samme transaksjon som anmodetAv)
  → anmodningsperiode.er_fjernarbeid_twfa
```

Kolonnen er **tri-state**: `1` = ja, `0` = nei, `NULL` = ikke besvart. Uttrekket teller kun `1`.
Skillet betyr noe også utenfor statistikken: `EessiService` sender bare feltet videre til A001 når
det ikke er null.

`anmodningsperiode` henger på `behandlingsresultat` — den durable saksmodellen — og har allerede
`unntak_fra_bestemmelse`, som er diskriminatoren for om flagget betyr noe (`ART_13_1_a`).
Presedens for kolonnen: `V7.6_07__saksbehandler_anmodet_om_unntak.sql` la til `anmodet_av` på samme
tabell for tilsvarende formål.

## Regelen som gjelder her

**Trenger du noe fra en prosessinstans varig — til statistikk, rapportering eller oppslag — så må
det lagres et annet sted i databasen.** `prosessinstans.data` er arbeidsdata for én saga, ikke en
saksmodell.

Fram til august 2026 var `prosessinstans.data` eneste lagringssted, og uttrekket måtte gjøre
`p.data LIKE '%erFjernarbeidTWFA=true%'` mot en CLOB på en tabell både entiteten
(`Prosessinstans.kt`) og `V3.0_01__PROSESSINSTANS.sql` kaller «Arbeidstabell for saksflyt», og som
`V161__prosessinstans_prioritet.sql` kaller **kortlevd**. Det virket utelukkende fordi ingen
slettejobb var implementert ennå. `V170` flyttet kilden, og `V171` backfillet historikken fra de radene.

Migreringene er bevisst delt i to: `V170` er en nullbar kolonne uten default, altså en ren
dictionary-oppdatering, mens `V171` skanner `prosessinstans.data`. Splitten handler om hva som skjer
hvis liveness-proben dreper podden midt i migreringen. Samlet ville Flyway mangle historikkraden mens
`ALTER`-en allerede var auto-committet av Oracle, og neste oppstart ville feilet på `ORA-01430`.
Delt krymper vinduet fra backfillens varighet til millisekundene mellom auto-commiten og
historikkraden, og `V171` kan trygt kjøres om igjen.

**Splitten fjerner ikke en fastlåst utrulling.** Flyways Oracle-dialekt melder
`supportsDdlTransactions() = false` for hele databasen, ikke per migrering, så en `V171` som kaster
gir en `success=0`-rad i `flyway_schema_history` selv om den bare inneholder DML — og appen starter
ikke igjen før noen kjører `flyway repair`. Det splitten gir, er en enkel opprydding: kolonnen er på
plass og registrert, så gjenopprettingen er å kjøre backfillen manuelt og deretter reparere. `V171`
har `er_fjernarbeid_twfa IS NULL` i begge setningene, så en slik manuell kjøring er trygg. Merk at
vakten ikke filtrerer noe ved førstegangskjøringen — kolonnen er tom da, og `V170` og `V171` kjører i
samme Flyway-run. Verdien ligger i re-kjøringer.

## Hvorfor `lagreAnmodningsperioder` bevarer flagget

`POST /anmodningsperioder/{behandlingID}` sletter og gjenoppretter periodene fra saksbehandlerens
skjema, og `AnmodningsperiodeSkrivDto.til()` kjenner kun skjemafeltene. Flagget settes derimot ved
anmodning (`registrerAnmodning`), så en redigering mellom anmodning og sending ville nullet det.

Ingenting sperrer for den redigeringen i det vinduet: `lagreAnmodningsperioder` garderer på
registrert svar og `sendt_utland`, og `Behandling.erRedigerbar()` på `ANMODNING_UNNTAK_SENDT` — men
**begge settes inne i `SendAnmodningOmUnntak`**, altså etter vinduet. På lykkelig sti er vinduet
sekunder; feiler et tidligere saga-steg, er det timer eller dager til noen restarter.

`lagreAnmodningsperioder` bærer derfor flagget over fra de eksisterende radene. Det er rotårsaken,
og den er fikset der — ikke med en reparasjon i sendeveien.

`SendAnmodningOmUnntak` leser kolonnen og bare den. Avlesningen ligger der, ikke i
`AbstraktSendUtland`: basisklassens `hentErFjernarbeidTWFA` returnerer null med vilje, fordi
`SendVedtakUtland` og `VideresendSoknad` deler `sendUtland` og flagget ikke skal lekke over på
vedtaks-SED-en eller A008. Det er ikke hypotetisk — en artikkel 16-sak som har fått svar får
lovvalgsperiode på samme behandlingsresultat som anmodningsperioden og havner i `SendVedtakUtland`.
Pinnet av tester i både `VideresendSoknadTest` og `SendVedtakUtlandTest`.

> **Kjent gap: rullerende deploy.** Dispatch er in-process (`ProsessinstansDispatcher` →
> `saksflytThreadPoolTaskExecutor`), så podden som oppretter prosessinstansen kjører også steget. En
> anmodning som håndteres av en pod med forrige versjon etter at `V170` har kjørt, får aldri satt
> kolonnen — den gamle podden leser prosessdataen direkte, sender riktig A001, og rører ikke
> kolonnen. Saken mangler da i statistikken. Vinduet er minutter og volumet titalls saker i året, så
> sannsynligheten er lav, men gapet er reelt og stille. Prosessinstans-radene slettes ikke, så
> `V171`s to UPDATE-setninger kan kjøres på nytt etter at utrullingen er ferdig hvis det trengs —
> `IS NULL`-vakten gjør en slik kjøring strengt additiv. Merk at `V171` **ikke** lukker dette gapet
> selv: den kjører i samme utrulling som `V170`, altså før en gammel pod rekker å lage en gap-rad.
> Skal gapet lukkes, må kjøringen gjøres manuelt etterpå eller legges i en senere release.
> `ProsessDataKey.ER_FJERNARBEID_TWFA` og skrivingen i `ProsessinstansBuilder` beholdes inntil
> videre nettopp som kilde for en slik ny kjøring.



## Replikering nuller flagget

`ReplikerBehandlingsresultatService.replikerAnmodningsperioder` bruker `BeanUtils.cloneBean`, som
kopierer alt. `er_fjernarbeid_twfa` nulles der eksplisitt, på linje med `sendtUtland` og
`anmodningsperiodeSvar`. Uten det ville en revurdering fått sin egen `behandling_id` og sitt eget
vedtak, og blitt telt som en ekstra sak — den gamle spørringen var immun mot dette fordi replikaen
aldri har en `ANMODNING_OM_UNNTAK`-prosessinstans.


## Kjent svakhet som ikke løses av kolonnen

`resultat_type` kan endres til `ANNULLERT` eller `HENLEGGELSE` i ettertid uten at vedtaksdatoen
fjernes, og behandlingen forsvinner da fra vedtaksåret sitt. **Tallene er derfor ikke reproduserbare
over tid.** Det krever en DVH-strøm med `funksjonell_tid` eller en snapshot-tabell, ikke en
kolonneflytting. Bør avklares med Medlemskap og avgift om «offisiell rapportering» betyr
reproduserbare tall.

## Kjent avgrensning

Uttrekket dekker kun saker der **Norge selv har sendt** anmodningen. Innkommende A001 fra andre land
er ikke med — verdien finnes på Kafka-meldingen fra melosys-eessi, men melosys-api sin
`AnmodningUnntak`-modell har ikke feltet.
Se [MELOSYS-8252](https://nav.atlassian.net/browse/MELOSYS-8252); avventer faglig avklaring om
slike saker overhodet skal telles.

## Tester

- `RammeavtaleStatistikkIT` — uttrekket mot ekte Oracle (tri-state, DISTINCT, vedtaksår, tidssone)
- `RammeavtaleBackfillIT` — kjører UPDATE-setningene fra `V171` mot seedet data, og pinner at `V170`
  kun legger til kolonnen. Leser SQL-en ut av migreringsfilene, så testen ikke kan komme i utakt med
  det som deployes
