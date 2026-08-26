# Statistikk — rammeavtale om fjernarbeid (TWFA)

Uttrekk til Medlemskap og avgift over saker behandlet etter rammeavtalen for fjernarbeid i EØS
(Telework Framework Agreement). Erstatter en manuell Excel-rutine. Se MELOSYS-8150.

## Hvor flagget kommer fra

Saksbehandlerens avhuking for «rammeavtale om fjernarbeid» lagres på kolonnen
`anmodningsperiode.er_fjernarbeid_twfa` (`V170__anmodningsperiode_fjernarbeid_twfa.sql`):

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
slettejobb var implementert ennå. `V170` flyttet kilden og backfillet historikken fra de radene.

## Midlertidig fallback i sendeveien

`SendAnmodningOmUnntak.hentErFjernarbeidTWFA` leser kolonnen, men faller tilbake til prosessdataen
når den er null, og logger på INFO når det skjer. Det dekker anmodninger opprettet av en pod med
forrige versjon i deploy-vinduet — uten fallbacken kunne A001 blitt sendt til utlandet uten
rammeavtale-flagget. **Fallbacken kan fjernes når loggmeldingen er borte fra prod**, sammen med
`ProsessDataKey.ER_FJERNARBEID_TWFA` og skrivingen i `ProsessinstansBuilder`.

Merk at avlesningen ligger i `SendAnmodningOmUnntak`, ikke i `AbstraktSendUtland`. Basisklassens
`hentErFjernarbeidTWFA` returnerer null med vilje: `SendVedtakUtland` og `VideresendSoknad` deler
`sendUtland`, og flagget skal ikke lekke over på vedtaks-SED-en eller A008 bare fordi behandlingen
har en anmodningsperiode. Det er pinnet av en test i `VideresendSoknadTest`.

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
- `RammeavtaleBackfillIT` — kjører UPDATE-setningene fra `V170` mot seedet data. Leser SQL-en ut av
  migreringsfila, så testen ikke kan komme i utakt med det som deployes
