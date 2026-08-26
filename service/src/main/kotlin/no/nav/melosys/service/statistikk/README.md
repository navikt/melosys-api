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
når den er null — og **skriver verdien tilbake til kolonnen**. Fallbacken alene ville reddet A001-en,
men latt raden stå null og saken forsvinne ut av rapporteringstallene; ingen jobb backfiller på nytt
etter at V170 har kjørt.

To tilfeller treffer den:

- anmodninger opprettet av en pod med forrige versjon i deploy-vinduet, og
- anmodningsperioder som ble slettet og gjenopprettet av `lagreAnmodningsperioder` mellom anmodning
  og sending. Den metoden sperrer kun på registrert svar og `sendt_utland`, og ingen av delene er
  satt i det vinduet — så en saksbehandler som redigerer perioden etter å ha anmodet nuller både
  `er_fjernarbeid_twfa` og `anmodet_av`. Tilbakeskrivingen reparerer flagget ved sending.

Fallbacken (og `ProsessDataKey.ER_FJERNARBEID_TWFA` + skrivingen i `ProsessinstansBuilder`) kan
fjernes når INFO-loggen «leser … fra prosessdataen og skriver den tilbake» har vært borte fra prod
gjennom en hel anmodningssyklus.

Merk at avlesningen ligger i `SendAnmodningOmUnntak`, ikke i `AbstraktSendUtland`. Basisklassens
`hentErFjernarbeidTWFA` returnerer null med vilje: `SendVedtakUtland` og `VideresendSoknad` deler
`sendUtland`, og flagget skal ikke lekke over på vedtaks-SED-en eller A008 bare fordi behandlingen
har en anmodningsperiode. Det er ikke hypotetisk — en artikkel 16-sak som har fått svar får
lovvalgsperiode på samme behandlingsresultat som anmodningsperioden og havner i `SendVedtakUtland`.
Pinnet av tester i både `VideresendSoknadTest` og `SendVedtakUtlandTest`.

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
- `RammeavtaleBackfillIT` — kjører UPDATE-setningene fra `V170` mot seedet data. Leser SQL-en ut av
  migreringsfila, så testen ikke kan komme i utakt med det som deployes
