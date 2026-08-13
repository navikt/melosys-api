# Duplikate saker ved samtidige digital-søknad-meldinger

**Status:** Implementert (MELOSYS-8151). Verifisert e2e mot lokal benk med `SLEEP=0` (alt sendt på
én gang): begge reproduksjons-skript gir nå **én sak** uten feilede prosessinstanser.
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

Tre lag som utfyller hverandre (defense-in-depth), verifisert e2e med `SLEEP=0`:

1. **Atomisk sak-resolusjon (melosys-api).** Ny cross-instance DB-lås `DIGITAL_SOKNAD_SAK_LOCK(aktoer_id)`
   (`DigitalSøknadSakLockRepository`, Flyway `V161`). NY-steget `OpprettSakOgBehandlingDigitalSøknad` tar
   låsen på aktørId, re-sjekker `finnGyldigSaksnummerForSkjemaIder(...)` under låsen, og fester på
   eksisterende sak hvis den finnes (delt `DigitalSøknadEksisterendeSakHåndterer`) — ellers oppretter sak.
   Markøren `DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE` får `OPPRETT_ARKIVSAK`/`OPPRETT_OPPGAVE` til å hoppe over.
2. **Symmetrisk «claim» (melosys-api).** Ved opprettelse av ny sak reserveres de øvrige relaterte
   skjemaId-ene mot saken (tomme mapping-rader, `SkjemaSakMappingService.claimRelaterteSkjemaIder`), slik
   at en del som prosesseres senere — også rot-innsendingen som ikke selv refererer de andre — finner saken
   uavhengig av rekkefølge.
3. **Full serialisering per gruppe.** skjema-api beregner en stabil `gruppeId` (id-en til det tidligst
   opprettede skjemaet i fnr+enhet+periode-gruppen) og legger den på `SkjemaMottattMelding`. melosys-api
   bruker `{gruppeId}_{skjemaId}` som låsreferanse, der `gruppeId` er `gruppePrefiks`. Den eksisterende
   (DB-baserte, cross-instance) PÅ_VENT-mekanismen serialiserer da hele gruppens flyt — én del om gangen —
   som også fjerner det sekundære `UQ_VILKAARSRESULTAT`-racet (samtidig `VURDER_INNGANGSVILKÅR` på delt
   behandling). gruppeId er en opak skjema-UUID → ingen person-ID i lås-nøkkelen.

## Avklaringer
- Personvern: serialiseringsnøkkel = `gruppeId` (opak skjema-UUID), ikke fnr/aktørId. DB-låsen låser på
  aktørId (intern pseudonym ID), som fagsak allerede nøkles på.
- Endringen spenner over melosys-api + melosys-skjema-api (+ `melosys-skjema-api-types`): det opprinnelige
  «melosys-api alene» ble utvidet bevisst for å få en personvernvennlig felles gruppe-ID.
