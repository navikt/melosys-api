# Statistikk — rammeavtale om fjernarbeid (TWFA)

Uttrekk til Medlemskap og avgift over saker behandlet etter rammeavtalen for fjernarbeid i EØS
(Telework Framework Agreement). Erstatter en manuell Excel-rutine. Se MELOSYS-8150.

## ⚠️ Kjent teknisk gjeld: kilden er en arbeidstabell

**`RammeavtaleStatistikkRepository` leser `prosessinstans.data`. Det er ikke en holdbar løsning, og
skal erstattes.**

Saksbehandlerens avhuking for «rammeavtale om fjernarbeid» lagres i dag kun ett sted: som
`erFjernarbeidTWFA=true` i `java.util.Properties`-teksten i CLOB-kolonnen `prosessinstans.data`
(`ProsessDataKey.ER_FJERNARBEID_TWFA`, satt i `AnmodningUnntakService`). Uttrekket må derfor gjøre
`p.data LIKE '%erFjernarbeidTWFA=true%'`.

Hvorfor det er feil:

- **`prosessinstans` er en arbeidstabell.** Både entiteten (`Prosessinstans.kt`) og
  `V3.0_01__PROSESSINSTANS.sql` kaller den «Arbeidstabell for saksflyt», og
  `V161__prosessinstans_prioritet.sql` sier rett ut at den er **kortlevd**. Den dagen noen
  implementerer oppryddingen designet inviterer til, forsvinner rapporteringstallene stille —
  uten at noen test eller alarm fanger det.
- **`LIKE` mot CLOB er utypet og uindeksert.** Ingen constraint, ingen kolonne, full scan.
- **Tallene er ikke reproduserbare over tid.** `resultat_type` kan endres til `ANNULLERT` eller
  `HENLEGGELSE` i ettertid uten at vedtaksdatoen fjernes, og behandlingen forsvinner da fra
  vedtaksåret sitt. Dette løses ikke av en kolonneflytting alene — det krever DVH-strøm med
  `funksjonell_tid` eller en snapshot-tabell.

Det virker i dag utelukkende fordi ingen slettejobb finnes ennå, og fordi `data` aldri nulles.
Det er en tilfeldighet, ikke en garanti.

## Regelen som følger av dette

**Trenger du noe fra en prosessinstans varig — til statistikk, rapportering eller oppslag — så må
det lagres et annet sted i databasen.** `prosessinstans.data` er arbeidsdata for én saga, ikke en
saksmodell.

## Planlagt fiks

Kolonne `er_fjernarbeid_twfa` på `anmodningsperiode`, satt fra `AnmodningUnntakService` i samme
transaksjon som `oppdaterAnmodetAvForBehandling`. Presedens finnes i samme tabell:
`V7.6_07__saksbehandler_anmodet_om_unntak.sql` la til `anmodet_av` for tilsvarende formål.
`anmodningsperiode` henger på `behandlingsresultat` — den durable saksmodellen — og har allerede
`unntak_fra_bestemmelse`, som er nettopp diskriminatoren for om flagget betyr noe.

Deretter skrives spørringen her om til å joine `anmodningsperiode`, og `AbstraktSendUtland` leser
flagget derfra i stedet for fra prosessdataen.

**Backfill er mulig så lenge prosessinstans-radene finnes.** Det går ingen klokke i dag — det finnes
ingen slettejobb for `prosessinstans`, bekreftet ved gjennomgang av `@Scheduled`, naisjobs og Flyway
i august 2026. Men avhengigheten består: blir oppryddingen designet inviterer til implementert før
migreringen kjøres, er historikken tapt. Excel-lista hos Medlemskap og avgift er da eneste kilde, og
RINA kan ikke brukes (`hentSedGrunnlag` returnerer `SedGrunnlagDto` uten feltet).

## Kjent avgrensning

Uttrekket dekker kun saker der **Norge selv har sendt** anmodningen. Innkommende A001 fra andre land
er ikke med — verdien finnes på Kafka-meldingen fra melosys-eessi, men melosys-api sin
`AnmodningUnntak`-modell har ikke feltet, og prosesstype-filteret ville uansett ikke matchet.
Se [MELOSYS-8252](https://nav.atlassian.net/browse/MELOSYS-8252); avventer faglig avklaring om
slike saker overhodet skal telles.
