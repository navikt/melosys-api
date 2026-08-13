# Testplan — MELOSYS-8151 duplikate saker ved samtidige digital-søknad-meldinger

Oversikt over hvilke testtilfeller vi ønsker å dekke for endringen, og status for hvert.
Se `duplikate-saker-digital-soknad.md` for selve designet.

Status: ✅ dekket · 🟡 delvis · ⬜ gjenstår

---

## 1. Lag 1 — atomisk sak-resolusjon (DB-lås på aktørId)

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 1.1 | To transaksjoner på samme aktørId serialiseres — B får ikke låsen før A committer | IT (Oracle) | ✅ | `DigitalSøknadSakLockIT` |
| 1.2 | Ulik aktørId blokkerer ikke hverandre | IT | ✅ | `DigitalSøknadSakLockIT` |
| 1.3 | `FOR UPDATE WAIT` gir opp etter ~10 s i stedet for å henge, med `PessimisticLockingFailureException` | IT | ✅ | `DigitalSøknadSakLockIT` |
| 1.4 | NY-flyten tar låsen FØR re-sjekk av eksisterende sak (rekkefølge) | Enhet | ✅ | `OpprettSakOgBehandlingDigitalSøknadTest` |
| 1.5 | Finner re-sjekken en sak under låsen, festes søknaden på den i stedet for å opprette ny | Enhet | ✅ | `OpprettSakOgBehandlingDigitalSøknadTest` |
| 1.6 | Ingen eksisterende sak → vanlig opprett-vei | Enhet | ✅ | `OpprettSakOgBehandlingDigitalSøknadTest` |
| 1.7 | NY-flyt som taper kappløpet ender på samme sak, ingen duplikat fagsak | IT | ✅ | `DigitalSøknadDuplikatSakIT` |
| 1.8 | `DigitalSøknadSakLås.lås` kaller `sikreLåsRad` før `taRadlås` | Enhet | ✅ | `DigitalSøknadSakLåsTest` |
| 1.9 | Feiler `sikreLåsRad` (samtidig opprettelse fra annen instans), går `lås` likevel videre til `taRadlås` | Enhet | ✅ | `DigitalSøknadSakLåsTest` |
| 1.10 | Feiler `taRadlås`, propagerer feilen — vi fortsetter ALDRI uten lås | Enhet | ✅ | `DigitalSøknadSakLåsTest` |
| 1.11 | Oppgave opprettes ikke under låsen; `OPPRETT_OPPGAVE`-steget tar den etterpå | Enhet | ✅ | `OpprettOppgaveTest`, `OpprettSakOgBehandlingDigitalSøknadTest` |
| 1.12 | `OPPRETT_ARKIVSAK` hoppes over når vi festet på eksisterende sak | Enhet | ✅ | `OpprettArkivsakTest` |
| 1.13 | Lås-raden mangler (`sikreLåsRad` ikke kjørt) → `IllegalStateException`, ikke stille videre | Enhet/IT | ⬜ | |

## 2. Lag 2 — claim av relaterte skjemaId-er

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 2.1 | Claim-rader skrives for relaterte skjemaId-er uten eksisterende mapping | Enhet | ✅ | `SkjemaSakMappingServiceTest` |
| 2.2 | Allerede mappede skjemaId-er overskrives ikke | Enhet | ✅ | `SkjemaSakMappingServiceTest` |
| 2.3 | Tom liste er en no-op | Enhet | ✅ | `SkjemaSakMappingServiceTest` |
| 2.4 | Rot-innsending som prosesseres sist finner saken via claim-raden | IT | ✅ | `DigitalSøknadDuplikatSakIT` |
| 2.5 | Claim-rad fylles med ekte data når delen faktisk mottas (`lagreMapping` oppdaterer, lager ikke duplikat rad) | Enhet/IT | ⬜ | Viktig: `skjemaId` er PK, så `save` skal bli en oppdatering |
| 2.6 | Claim-rader ekskluderes fra saksstatus-synken (`finnAlleSaksstatusSynkRader`) | IT | ✅ | `SaksstatusSynkProjeksjonIT` |
| 2.7 | Claim-rader ekskluderes fra `finnSaksstatusSynkRaderForSaksnummer` | IT | ✅ | `SaksstatusSynkProjeksjonIT` |
| 2.8 | Claim-rad blokkerer ikke sletting av mottatte opplysninger (FK-en fra MELOSYS-8135) | IT | ⬜ | |

## 3. Lag 3 — serialisering per søknadsgruppe

### 3a. gruppeId i melosys-skjema-api

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 3.1 | Første innsending i gruppen får seg selv som gruppeId | Enhet | ✅ | `InnsendingServiceTest` |
| 3.2 | Relatert del gjenbruker gruppeId fra første del | Enhet | ✅ | `InnsendingServiceTest` |
| 3.3 | **Tidligere opprettet utkast som sendes SIST får samme gruppeId** — stabilitet uavhengig av rekkefølge | Enhet | ✅ | `InnsendingServiceTest` |
| 3.4 | gruppeId persisteres på skjemaet | Enhet | ✅ | `InnsendingServiceTest` |
| 3.5 | Allerede tildelt gruppeId endres ikke ved ny prosessering (idempotens ved redelivery) | Enhet | ✅ | `InnsendingServiceTest` |
| 3.6 | To grupper som møtes (flere ulike gruppeId-er i samme gruppe) velges deterministisk | Enhet | ⬜ | Kantttilfelle, `eksisterende.size > 1`-grenen |
| 3.7 | Skjema uten utsendelsesperiode / uten data → ingen gruppe, faller tilbake | Enhet | ⬜ | |
| 3.8 | Backfill i `V21` setter `gruppe_id = id` for eksisterende SENDT-skjemaer | IT | ⬜ | |

### 3b. Låsreferanse og serialisering i melosys-api

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 3.9 | Låsreferansen blir `{gruppeId}_{skjemaId}` når gruppeId finnes | Enhet | ✅ | `ProsessinstansServiceTest` |
| 3.10 | Uten gruppeId blir referansen `{skjemaId}_{skjemaId}` (skjemaet er sin egen gruppe) | Enhet | ✅ | `ProsessinstansServiceTest`, `DigitalSøknadMottakIT` |
| 3.11 | `gruppePrefiks` er lik for to deler i samme gruppe med ulik skjemaId | Enhet | ✅ | `SøknadLåsReferanseTest` |
| 3.12 | Tre relaterte deler sendt samtidig → nøyaktig én fagsak, ingen feilede prosessinstanser | IT | ✅ | `DigitalSøknadSerialiseringIT` |
| 3.13 | Redelivery av samme skjema dedupliseres fortsatt på tvers av prosesstype | IT | ✅ | `DigitalSøknadRedeliveryDedupIT` |
| 3.14 | Serialiseringen fjerner `UQ_VILKAARSRESULTAT`-racet (samtidig `VURDER_INNGANGSVILKÅR`) | IT | 🟡 | indirekte i `DigitalSøknadSerialiseringIT` via «ingen feilede» |

### 3c. Bakoverkompatibilitet — kritisk

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 3.15 | Bar `{skjemaId}` (gammelt format) er fortsatt en gyldig SØKNAD-referanse | Enhet | ✅ | `SøknadLåsReferanseTest` |
| 3.16 | Gammel referanse beholder hele referansen som `gruppePrefiks` | Enhet | ✅ | `SøknadLåsReferanseTest` |
| 3.17 | ~~Gammel referanse grupperes sammen med ny~~ — viste seg feil, se 3.20 | — | ❌ | fjernet |
| 3.18 | **En prosessinstans i gammelt format som står PÅ_VENT velter ikke `ProsessinstansFerdigListener`** | Enhet | ✅ | `ProsessinstansFerdigListenerTest` |
| 3.20 | Gammelt og nytt format er IKKE samme gruppe (asymmetri i deploy-vinduet) | Enhet | ✅ | `SøknadLåsReferanseTest` |
| 3.19 | Ugyldig referanse kaster fortsatt med dekkende feilmelding | Enhet | ✅ | `SøknadLåsReferanseTest` |

## 4. Kafka-kompatibilitet (utrullingsrekkefølge)

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 4.1 | Melding uten `gruppeId` leses med `gruppeId = null` (gammel produsent, ny konsument) | Enhet | ✅ | `KafkaSerializationTest` |
| 4.2 | Melding med `gruppeId` leses korrekt | Enhet | ✅ | `KafkaSerializationTest` |
| 4.3 | Ukjent felt velter ikke konsumenten (ny produsent, gammel konsument) | Enhet | ✅ | `KafkaSerializationTest` |

## 5. Feilhåndtering og framdrift

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 5.1 | Feilet prosess med SØKNAD-referanse slipper fram neste i gruppen | Enhet | ✅ | `ProsessinstansFerdigListenerTest` |
| 5.2 | **Feilet prosess med annen låsreferansetype slipper IKKE fram noe** — dagens oppførsel bevart | Enhet | ✅ | `ProsessinstansFerdigListenerTest` |
| 5.3 | Feilet prosess uten låsreferanse er en no-op | Enhet | ✅ | `ProsessinstansFerdigListenerTest` |
| 5.4 | `ProsessinstansFeiletEvent` arver ikke fra `ProsessinstansFerdigEvent` (ellers fanger den eksisterende lytteren den, for alle prosesstyper) | Enhet | ⬜ | Kan dekkes som arkitekturtest |
| 5.5 | Rollback ved feil midt i flyten etterlater ikke halvferdig sak | IT | ✅ | `DigitalSøknadRollbackIT` |

## 6. Migreringer

| # | Testtilfelle | Nivå | Status | Hvor |
|---|---|---|---|---|
| 6.1 | Flyway-migreringene kjører uten versjonskollisjon | IT | ✅ | implisitt — hele IT-suiten laster Spring-konteksten |
| 6.2 | `DIGITAL_SOKNAD_SAK_LOCK` opprettes med riktig PK | IT | ✅ | implisitt via `DigitalSøknadSakLockIT` |

---

## Prioritering når vi tar tak i restansene

**Bør dekkes før merge:**
- 2.5 — at en claim-rad oppdateres og ikke duplikeres hviler på at `skjemaId` er PK. Verdt en eksplisitt test.

**Kan tas senere:**
- 3.6, 3.7, 3.8 — kantttilfeller i gruppeId-tildelingen.
- 1.13, 2.8, 5.4 — lav sannsynlighet eller dekket indirekte.

## Manuell verifikasjon som gjenstår

E2E mot lokal benk med `SLEEP=0` (alle deler sendt samtidig) bør kjøres på nytt etter at gruppeId
ble lagt om fra utregnet til persistert tildeling. Reproduksjonsskriptene ligger i
melosys-skjema-api (`scripts/reproduser-arvet-kobling.sh`). Forventning: én sak, ingen feilede
prosessinstanser.
