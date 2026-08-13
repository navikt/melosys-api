# Duplikate saker ved samtidige digital-søknad-meldinger

**Status:** Implementert (MELOSYS-8151). Enhets- og integrasjonstester grønne i begge repoer.
E2E mot lokal benk med `SLEEP=0` bør kjøres på nytt etter at gruppeId ble lagt om til persistert
tildeling.
**Gjelder:** melosys-api + melosys-skjema-api, mottak av digitale søknader (`UTSENDT_ARBEIDSTAKER`).
**Relatert:** [[skjema-sak-mapping]], MEL-16515 (journalføring/kobling — egen sak).

## Symptom

Når flere relaterte deler av samme søknad (arbeidstakers del, arbeidsgivers del,
og nye versjoner) sendes inn tett etter hverandre, opprettes det **flere fagsaker**
for det som skulle vært **én** sak. Tingene havner også i feil rekkefølge.

## Rotårsak (verifisert i koden)

Det er en **distribuert kappløpstilstand** — ikke at konsumenten leser flere meldinger
om gangen. Konsumenten leser allerede én og én:
`MAX_POLL_RECORDS=1`, concurrency=1, `ackMode=RECORD`
(`integrasjon/.../kafka/KafkaConfig.kt:130,148`).

Tre ting samvirker:

1. **«Ny vs. eksisterende sak» avgjøres ved konsum**, men grunnlaget committes asynkront.
   `DigitalSøknadMottattConsumer.kt:37` kaller `finnGyldigSaksnummerForSkjemaIder(...)`,
   men `SkjemaSakMapping`-raden som dette leser, skrives først **inne i den asynkrone
   sagaen** (`OpprettSakOgBehandlingDigitalSøknad` → `lagreSkjemaSakMapping`). Sagaen
   kjøres på en trådpool (`ProsessinstansDispatcher` → `saksflytThreadPoolTaskExecutor`),
   så konsumenten returnerer før saken finnes. Neste melding ser ingen sak → lager ny.

2. **Serialiseringen finnes, men på feil nøkkel.** `ProsessinstansBehandlerDelegate`
   setter en prosess PÅ_VENT hvis en annen aktiv prosess har samme `gruppePrefiks`.
   For digital søknad er `låsReferanse = skjemaId`, og
   `SøknadLåsReferanse.gruppePrefiks = hele skjemaId` (`SøknadLåsReferanse.kt:12`).
   Dermed serialiseres **kun redeliveries av samme skjema** — aldri de relaterte
   delene (v1/AG/v2) av samme søknad.

3. **Kafka-nøkkelen er `skjemaId`** (`melosys-skjema-api/.../SkjemaMottattProducerKafka.kt`:
   `val key = skjemaMottattMelding.skjemaId.toString()`). Med **flere instanser** havner
   relaterte meldinger på ulike partisjoner/instanser og prosesseres ekte parallelt —
   så ren in-JVM-serialisering løser det uansett ikke.

## Empirisk bevis (lokal benk, local-mock)

Tre innsendinger for **samme** arbeidstaker (HANS, `01816023404`), **samme** org
(Ståles Stål, `999999999`), **samme** periode — kjørt via
`melosys-skjema-api/scripts/reproduser-arvet-kobling.sh` og verifisert i
`SKJEMA_SAK_MAPPING`:

| Kjøring            | v1    | AG    | v2    | Resultat                |
|--------------------|-------|-------|-------|-------------------------|
| Uten pause         | MEL-3 | MEL-4 | MEL-2 | **3 ulike saker** (bug) |
| Med 4 s pause      | MEL-5 | MEL-5 | MEL-5 | **Én sak** (mitigert)   |

## Valgt løsning: atomisk sak-resolusjon med DB-lås (strategi 1)

Robust på tvers av instanser og uavhengig av Kafka-rekkefølge. Fikser også rekkefølgen:
del 2 venter til del 1 har committet sak + mapping, og fester seg da på riktig sak.

**Personvern:** Vi låser på **aktørId** (intern, pseudonym person-ID som fagsak allerede
nøkles på), ikke fnr. aktørId resolves allerede inne i sagaen og brukes ikke «løst» til
noe nytt.

### Skisse

1. **Ny cross-instance lås (Oracle).** Tabell
   `DIGITAL_SOKNAD_SAK_LOCK(aktoer_id VARCHAR PK, opprettet TIMESTAMP)` via Flyway.
   Lås-hjelper: sørg for at raden finnes (`MERGE`), så `SELECT … FOR UPDATE` på
   `aktoer_id`. Holdes til transaksjonen committer.
2. **Flytt sak-resolusjonen inn i den låste seksjonen.** I `OpprettSakOgBehandlingDigitalSøknad`
   (NY-flyten): resolv aktørId → ta lås(aktørId) → kjør `finnGyldigSaksnummerForSkjemaIder(...)`
   på nytt. Finnes sak nå → deleger til eksisterende-flyten; ellers opprett sak + lagre
   mapping — alt i samme `@Transactional` som låsen.
3. **Sikkerhetsnett (vurderes):** DB-constraint mot to samtidige åpne digital-søknad-saker
   for samme relasjon.

### Tester
- Integrasjonstest: to relaterte `SkjemaMottattMelding` (v1 + AG) ~samtidig → **én** fagsak,
  AG på samme sak. Skal feile før fiks.
- Enhetstest for låsen (to tråder, samme aktørId → seriell).
- Behold/utvid `DigitalSøknadEksisterendeSakIT`.

### Berørte filer (foreløpig)
- `service/.../soknad/DigitalSøknadMottattConsumer.kt`
- `saksflyt/.../steg/soknad/OpprettSakOgBehandlingDigitalSøknad.kt` (+ `HåndterEksisterendeSakDigitalSøknad.kt`)
- `service/.../sak/SkjemaSakMappingService.kt`
- Ny lås-klasse + Flyway-migrering + tester

## Midlertidig demping (til fiksen er på plass)

Ved manuell testing/reproduksjon: vent **3–4 sekunder mellom hver innsending**, slik at
sagaen for den forrige rekker å committe sak + mapping før neste melding konsumeres.
Da unngår man duplikatsakene. Innebygd i
`melosys-skjema-api/scripts/reproduser-arvet-kobling.sh`.

## Implementasjon (MELOSYS-8151)

Tre lag som utfyller hverandre (defense-in-depth):

1. **Atomisk sak-resolusjon (melosys-api).** Ny cross-instance DB-lås `DIGITAL_SOKNAD_SAK_LOCK(aktoer_id)`
   (`DigitalSøknadSakLockRepository` + `DigitalSøknadSakLås`, Flyway `V169`). NY-steget
   `OpprettSakOgBehandlingDigitalSøknad` tar låsen på aktørId, re-sjekker
   `finnGyldigSaksnummerForSkjemaIder(...)` under låsen, og fester på eksisterende sak hvis den
   finnes (delt `DigitalSøknadEksisterendeSakHåndterer`) — ellers oppretter sak. Markøren
   `DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE` får `OPPRETT_ARKIVSAK` til å hoppe over, siden den
   eksisterende saken allerede har arkivsak.
2. **Symmetrisk «claim» (melosys-api).** Ved opprettelse av ny sak reserveres de øvrige relaterte
   skjemaId-ene mot saken (tomme mapping-rader, `SkjemaSakMappingService.claimRelaterteSkjemaIder`), slik
   at en del som prosesseres senere — også rot-innsendingen som ikke selv refererer de andre — finner saken
   uavhengig av rekkefølge.
3. **Full serialisering per gruppe.** skjema-api tildeler en stabil `gruppeId` som legges på
   `SkjemaMottattMelding`. melosys-api bruker `{gruppeId}_{skjemaId}` som låsreferanse, der
   `gruppeId` er `gruppePrefiks`. Den eksisterende (DB-baserte, cross-instance) PÅ_VENT-mekanismen
   serialiserer da hele gruppens flyt — én del om gangen — som også fjerner det sekundære
   `UQ_VILKAARSRESULTAT`-racet (samtidig `VURDER_INNGANGSVILKÅR` på delt behandling). gruppeId er en
   opak skjema-UUID → ingen person-ID i lås-nøkkelen.

### gruppeId må persisteres, ikke regnes ut

gruppeId er «id-en til det tidligst opprettede skjemaet i gruppen», men den kan ikke regnes ut på
nytt per melding. Gruppen ses via skjemaer med status `SENDT`, så et tidligere opprettet **utkast**
som sendes inn senere flytter «tidligst opprettede» og endrer gruppe-ID-en underveis:

| Hendelse | Gruppe sett fra avsender | Utregnet gruppeId |
|---|---|---|
| AG sendes (v1 er fortsatt utkast) | `{AG}` | `AG` |
| v1 sendes senere | `{v1, AG}` | `v1` — ulik! |

Det er nøyaktig rot-innsendings-scenarioet serialiseringen skal fange. Derfor tildeles gruppeId én
gang og lagres på `skjema.gruppe_id` (skjema-api, Flyway `V21`): finnes det allerede en gruppe-ID på
noen i gruppen — også på et utkast — gjenbrukes den; ellers tildeles tidligst opprettede med
skjema-ID som deterministisk tie-break.

Restrisiko: to deler som sendes helt samtidig og ikke ser hverandre kan tildele hver sin gruppe-ID.
Da faller vi tilbake til dagens oppførsel (ingen kryss-serialisering), og lag 1 fanger duplikatsaken.

### Kompatibilitet og utrulling

- `gruppeId` er nullable. Mangler det, er skjemaet sin egen gruppe: låsreferansen blir
  `{skjemaId}_{skjemaId}`, altså ingen kryss-serialisering med andre deler.
- Låsreferanse-formatet er bakoverkompatibelt: `LåsReferanseType.SØKNAD` godtar både
  `{gruppeId}_{skjemaId}` og bar `{skjemaId}`. Det er **påkrevd**, ikke pynt:
  `ProsessinstansFerdigListener` parser låsreferansen til alle prosessinstanser som står PÅ_VENT ved
  hvert ferdig-event, så én gammel rad i det gamle formatet ville kastet exception og stanset
  opplåsingen for alle prosesstyper.
- **Asymmetri i deploy-vinduet.** De to låsreferanse-formatene grupperes ikke sømløst mot hverandre.
  `ProsessinstansFerdigListener` sammenligner `gruppePrefiks` med LIKHET (`X` != `X_`), mens
  PÅ_VENT-oppslaget bruker `startsWith` (`X` fanger `X_Y`). Gamle rader og nye havner derfor ikke i
  samme gruppe ved opplåsing. Konsekvensen er avgrenset til utrullingsvinduet: en redelivery av et
  skjema som allerede har en aktiv prosessinstans i gammelt format kan i teorien få opprettet en
  ny prosessinstans, siden dedupen (`existsByLåsReferanseAndTypeIn`) sammenligner referansen
  eksakt og `X` != `X_X`. Vurdert som akseptabelt: vinduet er kort, og lag 1 (DB-låsen) hindrer
  uansett at det blir duplikate saker.
- `spring.jackson.deserialization.fail-on-unknown-properties: false` gjør at rekkefølgen på utrulling
  ikke velter konsumenten. Dekket av `KafkaSerializationTest`.
- Anbefalt rekkefølge likevel: **melosys-api først**, deretter skjema-api.

### Andre forhold håndtert

- **Låsen holdes ikke over HTTP-kall.** Attach-veien fra NY-flyten kaller
  `DigitalSøknadEksisterendeSakHåndterer.håndter(..., opprettOppgave = false)`; oppgaven opprettes av
  det etterfølgende `OPPRETT_OPPGAVE`-steget, utenfor låsen. WebClient har ingen response-timeout, så
  et hengende Oppgave-kall ville ellers kunne holdt en Oracle-radlås i det uendelige.
- **`FOR UPDATE WAIT 10`** i stedet for ubegrenset venting.
- **Claim-rader ekskluderes fra saksstatus-synken** (`originalData is not null` i
  `SAKSSTATUS_SYNK_PROJEKSJON`). Ellers ville skjema som ennå ikke er mottatt rapportert saksstatus
  tilbake til skjema-api.
- **Feilet prosess blokkerer ikke gruppen.** `ProsessinstansFeiletEvent` slipper fram neste i
  gruppen, avgrenset til SØKNAD-låsreferanser slik at andre prosesstyper beholder dagens oppførsel.

## Avklaringer
- Personvern: serialiseringsnøkkel = `gruppeId` (opak skjema-UUID), ikke fnr/aktørId. DB-låsen låser på
  aktørId (intern pseudonym ID), som fagsak allerede nøkles på.
- Endringen spenner over melosys-api + melosys-skjema-api (+ `melosys-skjema-api-types`): det opprinnelige
  «melosys-api alene» ble utvidet bevisst for å få en personvernvennlig felles gruppe-ID.

## Gjenstår
- Lås-tabellen `DIGITAL_SOKNAD_SAK_LOCK` vokser monotont (én rad per aktørId). Ingen opprydding er
  satt opp; radene er små, men bør ryddes hvis tabellen blir stor.
