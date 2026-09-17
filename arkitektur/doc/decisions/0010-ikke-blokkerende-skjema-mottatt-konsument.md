# 10. Ikke-blokkerende skjema-mottatt-konsument (MELOSYS-8290)

Date: 2026-09-16

## Status

Foreslått

## Context

`DigitalSøknadMottattConsumer.mottaSkjemaMelding` (Kafka-lytter for topic
`teammelosys.skjema.innsendt.v1`, gruppe `teammelosys-skjema-mottatt-consumer`)
kaller `SkjemaSakMappingService.finnMappetSaksnummerForSkjemaIder(alleIder)`
**synkront, før** noen `Prosessinstans` opprettes. Denne metoden kaster
`IllegalStateException` hvis den finner mer enn én gyldig åpen sak (status
`OPPRETTET`/`LOVVALG_AVKLART`) blant fagsakene knyttet til `skjemaId` +
`relaterteSkjemaIder` — et sjeldent datainkonsistens-scenario.

Containerfactory for denne konsumenten (`aivenSkjemaMottattContainerFactory` i
`integrasjon/.../kafka/KafkaConfig.kt`) bruker `SkippableKafkaErrorHandler`, som
arver `CommonContainerStoppingErrorHandler`: en ufanget exception i lytteren
**stopper hele containeren** for partisjonen. Konsekvensen er at:

1. Meldingen som utløste feilen aldri fører til at noen sak/behandling opprettes
   (exception kastes før `opprettProsessinstans*` kalles).
2. **Alle** senere meldinger på samme partisjon stopper opp — konsumenten
   "blokkerer" — helt til noen manuelt kaller `/admin/kafka/errors/{key}/retry`
   eller `/skip`.
3. Feilen er kun synlig i en in-memory-cache (`failedMessages`) på den poden som
   faktisk traff feilen, og forsvinner ved pod-restart.

Dette ble oppdaget ved å spore en konkret skjemaId der produsentloggen i
melosys-skjema-api bekreftet vellykket sending (`partition=0, offset=1231`), mens
`DigitalSøknadMottattConsumer` aldri logget mottak — verken info eller feil — i
tidsvinduet rundt sendingen.

## Decision

Vi skiller **routing** (skal denne meldingen behandles som ny sak eller
eksisterende sak?) fra **konsistens-validering** (finnes det faktisk flere åpne
saker samtidig — en datafeil som bør undersøkes), og flytter sistnevnte til et
sted der den ikke kan blokkere Kafka-konsumeringen.

### 1. Routing i konsumenten kaster ikke lenger
`SkjemaSakMappingService.finnMappetSaksnummerForSkjemaIder` slutter å kaste
`IllegalStateException` ved flere gyldige saker. Den velger i stedet ett
saksnummer deterministisk (første i den gyldige listen) slik at konsumenten
alltid kan avgjøre om `MELOSYS_MOTTAK_DIGITAL_SØKNAD` (ny sak) eller
`MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD` (eksisterende sak) skal opprettes.
Prosessinstansen opprettes dermed **alltid**, uavhengig av om det finnes en
datainkonsistens i bunnen.

### 2. Ambiguitets-sjekken flyttes inn i saga-steget
Selve valideringen — «fantes det egentlig flere gyldige åpne saker?» — gjøres nå
i `HåndterEksisterendeSakDigitalSøknad` (steg `HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD`,
del av `MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD`-flyten). Dette steget kjøres
av saksflyt-motoren **AFTER_COMMIT**, i en egen, senere transaksjon, helt
frikoblet fra Kafka-lytterens tråd (se saksflyt-skillets beskrivelse av
`ProsessinstansOpprettetEvent` → `ProsessinstansBehandler`). Hvis steget kaster
her, settes **kun denne ene prosessinstansen** til `FEILET` — synlig og
reprosesserbar via vanlig saksflyt-verktøy — uten at andre Kafka-meldinger eller
partisjoner påvirkes.

### 3. Relaterte skjemaId-er sendes eksplisitt med i prosessinstansen
For at steget skal kunne gjøre samme oppslag på nytt, legges en ny nøkkel til i
`ProsessDataKey`: `RELATERTE_SKJEMA_IDER`. Verdien (`melding.relaterteSkjemaIder`,
et øyeblikksbilde fra selve Kafka-meldingen) settes av `ProsessinstansService`
ved opprettelse (`opprettSøknadProsessinstans`), og leses ut igjen i
`HåndterEksisterendeSakDigitalSøknad` via
`prosessinstans.finnData<List<UUID>>(ProsessDataKey.RELATERTE_SKJEMA_IDER, emptyList())`.

Dette er trygt selv om relasjonene mellom skjema endrer seg senere: enhver
endring i "relatert"-status kommer i så fall som en **ny innsending** (nytt
skjemaId), som utløser en **ny** Kafka-melding med sitt eget ferske
`relaterteSkjemaIder`-snapshot. Hver innsending er dermed selvstendig konsistent
med sitt eget øyeblikksbilde — det er ikke behov for at eldre prosessinstanser
skal reflektere senere endringer.

### Ikke i scope
En bredere redesign av `SkippableKafkaErrorHandler` (f.eks. retry + dead-letter,
enten via egen Kafka-topic eller database-tabell) for å gjøre *alle* fem
konsumenter i `KafkaConfig` ikke-blokkerende, er vurdert og diskutert, men holdes
utenfor denne endringen. Denne ADR-en løser kun det konkrete, observerte
blokkerings-scenarioet for skjema-mottatt-konsumenten via `SkjemaSakMappingService`.

## Consequences

**Positive**
- Ambiguøse/datainkonsistente skjema-innsendinger blokkerer ikke lenger hele
  skjema-mottatt-partisjonen for alle andre innsendinger.
- Prosessinstansen opprettes alltid ved mottak — feil oppdages og kan undersøkes
  via saksflyt sitt vanlige `FEILET`-sporingsapparat, ikke en flyktig
  in-memory-cache bundet til én spesifikk pod.
- Ingen endring i selve forretningsregelen (fortsatt en feil hvis det finnes
  flere åpne saker) — kun *hvor* og *når* den håndheves.

**Avveininger**
- Oppslaget mot `skjemaSakMappingRepository`/`fagsakRepository` gjøres nå to
  ganger for samme melding (én gang for routing i konsumenten, én gang for
  validering i saga-steget) — akseptabelt gitt lavt volum og at det fjerner en
  langt dyrere kostnad (blokkert partisjon).
- `RELATERTE_SKJEMA_IDER` er et øyeblikksbilde fra meldingstidspunktet, ikke en
  levende referanse — se begrunnelse over for hvorfor dette er trygt.
- Løser kun skjema-mottatt-konsumenten spesifikt; de fire andre konsumentene i
  `KafkaConfig` beholder dagens container-stoppende feilhåndtering inntil videre.

## Filer

- `service/src/main/kotlin/no/nav/melosys/service/sak/SkjemaSakMappingService.kt`
  — fjerner kasting fra `finnMappetSaksnummerForSkjemaIder`.
- `service/src/main/kotlin/no/nav/melosys/service/soknad/DigitalSøknadMottattConsumer.kt`
  — uendret routing-logikk, men ikke lenger avhengig av at oppslaget kan kaste.
- `saksflyt-api/src/main/java/no/nav/melosys/saksflytapi/domain/ProsessDataKey.java`
  — ny konstant `RELATERTE_SKJEMA_IDER`.
- `saksflyt-api/src/main/java/no/nav/melosys/saksflytapi/ProsessinstansService.java`
  — `opprettSøknadProsessinstans` setter `RELATERTE_SKJEMA_IDER`.
- `saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/steg/soknad/HåndterEksisterendeSakDigitalSøknad.kt`
  — leser `RELATERTE_SKJEMA_IDER`, gjør ambiguitets-sjekken, kaster ved flere
  gyldige åpne saker.
- Berørte tester: `SkjemaSakMappingServiceTest.kt`,
  `HåndterEksisterendeSakDigitalSøknadTest.kt`, ev.
  `ProsessinstansServiceTest.kt`.
