# Saksstatus-synk til melosys-skjema-api — beslutningsgrunnlag

**Status:** Implementert (MELOSYS-8084, PR #3425, merget 2026-07-27). I prod.
**Gjelder:** melosys-api → melosys-skjema-api, løpende synk av saksstatus for saker med
skjema-kobling, + admin-massesynk (`POST /admin/skjema-saksstatus/synk`).
**Relatert:** [[duplikate-saker-digital-soknad]] (motsatt retning: skjema-api → melosys-api),
`skjema_sak_mapping`.

Dette dokumentet er ettersendt. PR-en oppga konklusjonene, ikke resonnementet bak dem, og
utredningen lå i en arbeidsmappe utenfor repoet. Det er rettet opp her.

## Problemet

Innsendte skjema i melosys-skjema-api viste ingenting om hva som skjedde med saken etterpå.
Brukere så «venter på arbeidsgiver» i måneder etter at saken var ferdigbehandlet, og vi kunne
ikke skille reelle etterslep fra saker som for lengst var lukket. Målingen bekreftet omfanget:
av 694 innsendinger var 222 deler markert som ventende hos oss mens saken var avsluttet i
Melosys — nær halvparten av «etterslepet» var ikke etterslep.

Melosys-api må derfor fortelle skjema-api når en fagsak endrer status. Spørsmålet var hvordan,
uten at et glemt kodested gir stille avvik, og uten at en feilende integrasjon kan velte
saksbehandlingen.

## Hvorfor ikke Kafka?

Dette er det spørsmålet som oftest stilles, og det fortjener et ordentlig svar.

**Mottakeren er ikke en Kafka-konsument.** melosys-skjema-api har i dag kun *produsenter*
(`SkjemaMottattProducerKafka`, `BrukervarselProducerKafka`) — ingen `@KafkaListener` finnes i
kodebasen. Å ta imot saksstatus over Kafka ville krevd ny konsument-infrastruktur der:
consumer factory, listener-container, feilhåndtering, retry/DLQ, offset-håndtering, samt ny
topic med tilgangsstyring.

**HTTP-integrasjonen fantes allerede.** `MelosysSkjemaApiClient` ble bygget for
saksnummer-callbacken (`SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API`), med auth-oppsett på plass og
i drift. Valget sto altså ikke mellom to likeverdige transporter, men mellom å utvide en
fungerende integrasjon og å innføre en ny transportmekanisme i mottakersystemet.

**Presedens internt:** melosys-api bruker allerede prosessinstans — ikke Kafka — for utgående
integrasjon mot faktureringskomponenten (`OPPDATER_FAKTURAMOTTAKER`).

**Presisering:** bestillingen er *ikke* synkron. Eventet oppretter en prosessinstans i samme
transaksjon som statusendringen; instansen plukkes opp asynkront, og det er kun selve
HTTP-leveransen inne i steget som er synkron. Transaksjonelt oppfører dette seg som en outbox.

*Motargumentet står likevel:* Kafka ville gitt løsere kobling — melosys-api slipper å kjenne
til skjema-api i det hele tatt — og holdbarhet uten prosessinstans-maskineri. Er skjema-api
nede, feiler steget og må rekjøres; en Kafka-melding ville bare ligget og ventet. Vurderingen
var at rekjørbarheten i console dekker dette godt nok til at en ny transport ikke var verdt
kostnaden. Blir det flere konsumenter av saksstatus senere, bør valget tas opp igjen.

## Hook-mekanismen: tre alternativer

### 1. SAGA-steg i hver prosessflyt — forkastet

Husets dominerende mønster, og derfor det første som ble utredet. Forkastet fordi det ikke
rekker: flertallet av stedene som endrer fagsak-status skjer *utenfor* en prosessinstans —
REST-kall fra melosys-web, admin-ruting, scheduler-jobber. Der finnes ingen flyt å legge et
steg inn i. Av de 11 kallstedene i produksjonskode ligger 7 utenfor prosessflyt:

| Kallsted | Kjører i prosessflyt |
|---|---|
| `AngiBehandlingsresultatService` | nei |
| `AvslagService` | nei |
| `AvsluttArt13BehandlingService` | nei |
| `FerdigbehandleService` | nei |
| `HenleggelseService` (×2) | nei |
| `VideresendSoknadService` | nei |
| `AvsluttFagsakOgBehandling` (×2) | ja |
| `AdminSedRuter` (×2) | ja |

En ren SAGA-løsning ville dekket de fire nederste og latt resten drive stille ut av synk.

### 2. Event-listener med direkte HTTP-kall — forkastet

Dette var faktisk førstevalget, og ble skrevet ned som konklusjon før det ble revidert. To
problemer: kallet skjer utenfor transaksjonen, så en krasj mellom commit og HTTP-kall taper
oppdateringen uten spor; og en feilet synk blir en loggmelding ingen ser, ikke noe rekjørbart.
Første utkast lyttet dessuten på *behandlings*-eventer, som viste seg ikke å dekke henleggelse
og annullering.

### 3. Event → én-stegs prosessinstans — valgt

`FagsakService.oppdaterStatus` publiserer `FagsakStatusEndretEvent`. Listeneren bestiller en
én-stegs prosessinstans `SYNK_SKJEMA_SAKSSTATUS` i samme transaksjon som statusendringen;
HTTP-kallet skjer i steget. Bestillingen er dermed outbox-atomisk, og feilede synker er
synlige og rekjørbare i melosys-console.

**Mønsteret er ikke innført her.** Kodebasen har tolv event-listenere i produksjonskode fra
før, flere på sak- og behandlingsnivå (`BehandlingEventListener`, `FagsakEventListener`,
`SaksoppplysningEventListener`, `UtstedtA1EventListener`). Nærmeste parallell er
`FaktureringEventListener`, som lytter på `BehandlingEndretStatusEvent` og bestiller en
prosessinstans framfor å kalle nedstrøms tjeneste direkte. Begrunnelsen står allerede som
kommentar i den koden, skrevet uavhengig av dette arbeidet:

> Bestill prosess i stedet for å kalle faktureringskomponent direkte, for å få støtte for
> feilhåndtering og rekjøring

Det er den samme avveiningen. SAGA er mønsteret for arbeid som hører til én bestemt prosess;
event-listener med prosessbestilling er mønsteret for tverrgående reaksjoner på en
statusendring.

### Funnet som gjorde valget mulig

`Fagsak.setStatus` har nøyaktig **én** kaller i hele kodebasen: `FagsakService.oppdaterStatus`
(`FagsakService.java:294`). Ett event ett sted gir dermed full dekning — i motsetning til
SAGA-varianten, der dekningen avhenger av at ingen glemmer et steg. En ArchUnit-regel
(`ArkitekturTestIT`) forbyr andre klasser å kalle `setStatus`, slik at invarianten holder over
tid. En egen test håndhever at synk-steget ligger i alle avslutt-flyter.

## Kostnaden ved den påkrevde parameteren

`oppdaterStatus` og `avsluttFagsakOgBehandling` tar en påkrevd `SkjemaSaksstatusSynk` uten
defaultverdi, så hvert kallsted må ta stilling til om synken håndteres av flyten det står i
(`HÅNDTERES_AV_PROSESSFLYT`) eller skal utløses av eventet (`SYNKRONISER`). Formålet er å
hindre at et prosessinstans-steg bestiller en barneprosess.

**Dette er den svakeste delen av designet, og det bør sies rett ut.** En integrasjonsdetalj som
navngir et satellittsystem har lekket inn i signaturen til en sentral domenemetode. Alle 11
kallsteder må importere `SkjemaSaksstatusSynk` — inkludert `TrygdeavtaleVedtakService`, som
behandler trygdeavtale-saker som aldri kan være skjema-koblet, og der kallet i tillegg er
merket som ubrukt (`TrygdeavtaleVedtakService.java:112`). Det er også hovedgrunnen til at
PR-en ble så bred: mesteparten av spredningen i diffen er denne signaturendringen, ikke
synk-logikken.

Alternativet var å **alltid** publisere eventet og la synken være idempotent — den er allerede
idempotent, og massesynken utnytter det. Da hadde ingen kallsteder trengt endring, og
dobbeltsynk fra en prosessflyt ville vært ufarlig, bare litt støyende. Prisen er at
SAGA-prinsippet om at steg ikke bestiller barneprosesser mykes opp.

Vi valgte eksplisitthet framfor idempotens-tillit. Er teamet uenig i den avveiningen, er
endringen tilbake overkommelig: fjern parameteren, publiser alltid, og la
`SkjemaSaksstatusSyncService` filtrere bort saker som allerede er synket i inneværende flyt.

## Bevisst akseptert

- **Massesynken kjører synkront i admin-tråden.** Forsvarlig ved dagens volum; endepunktet er
  et manuelt backfill-verktøy, ikke en løpende jobb.
- **Crash-vindu i SED-annullering.** Massesynken er sikkerhetsnettet.
- **Ingen kill-switch-toggle.** Mottaket i skjema-api er passivt, feilhåndteringen skal aldri
  velte saksflyt, og visningen kan stoppes på skjema-siden.
- **Ren fagsak-status-mapping** — behandlingsnivå bevisst utelatt (produkteierbeslutning
  2026-07-21, tatt etter manuell test).

## Kjent svakhet, ikke lukket

Mappingen ser kun på fagsak-status. En ny søknad på en allerede avsluttet sak oppretter en
NY_VURDERING-behandling uten at fagsak-status endres (`HåndterEksisterendeSakDigitalSøknad`),
så saken kan vise «Avsluttet» mens en behandling er åpen. Bør vurderes sammen med spørsmålet
om behandlingsnivået likevel hører hjemme i mappingen.
