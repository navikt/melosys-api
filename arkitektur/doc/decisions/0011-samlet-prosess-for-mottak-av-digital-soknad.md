# 11. Samlet prosess for mottak av digital søknad (MELOSYS-8151)

Date: 2026-10-09

## Status

Foreslått. Erstatter routingen i [ADR 0010](0010-ikke-blokkerende-skjema-mottatt-konsument.md) pkt. 1.

## Context

`DigitalSøknadMottattConsumer` valgte mellom ny sak og eksisterende sak ved å slå opp
`skjema_sak_mapping` for `skjemaId` + `relaterteSkjemaIder`. Mappingen lagres først når
sagaen har opprettet saken. Hvis to relaterte søknader (f.eks. arbeidsgivers og
arbeidstakers del) mottas tett på hverandre, finnes ikke mappingen når den andre
meldingen konsumeres. Da opprettes to saker.

Valget skjedde altså før den første prosessen var ferdig. Det trengs en
serialisering av relaterte søknader, slik at avgjørelsen tas etter at forrige søknad har
lagret mappingen.

## Decision

### 1. Én prosesstype som avgjør sak i sagaen
Konsumenten oppretter alltid `MELOSYS_MOTTAK_DIGITAL_SØKNAD`. Steget
`AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD` gjør mapping-oppslaget og bruker enten
`DigitalSøknadEksisterendeSakHåndterer` eller `DigitalSøknadNySakOppretter`.
Resultatet lagres i `DIGITAL_SØKNAD_NY_SAK`. `OPPRETT_ARKIVSAK_DIGITAL_SØKNAD` og
`OPPRETT_OPPGAVE_DIGITAL_SØKNAD` gjør bare noe for ny sak, og gjenbruker
`OpprettArkivsak` og `OpprettOppgave`.

### 2. Relaterte søknader serialiseres med eksisterende låsmekanisme
Låsreferansen er `{gruppe}_{skjemaId}` (`SøknadLåsReferanse`). Gruppen arves fra
prosessinstansen til et relatert skjema. Finnes ingen, er gruppen skjemaets egen id.
Saksflyt setter allerede prosesser `PÅ_VENT` når en annen aktiv prosess har samme
gruppeprefiks, og frigjør dem én om gangen når den foregående er `FERDIG`. Den andre
søknaden avklarer derfor sak først når mappingen fra den første finnes.

Dette forutsetter at:
- den senere søknaden har den tidligere i `relaterteSkjemaIder`,
- topicet har én partisjon, slik at den tidligere prosessinstansen er opprettet før
  den senere meldingen konsumeres.

Redelivery dedupliseres på `skjemaId` (slutten av låsreferansen) på tvers av alle
digital-søknad-typene, både i gammelt og nytt låsformat.

### 3. Overgang
`V172` gir den gamle ny-sak-flyten navnet `MELOSYS_MOTTAK_DIGITAL_SØKNAD_NY_SAK` og flytter
eksisterende prosessinstanser dit. `MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD` beholdes.
Begge er utgått, og flytene kan fjernes når ingen uferdige prosessinstanser bruker dem.
Enum-verdiene og kodeverk-radene beholdes for historikk.

## Consequences

**Positive**
- Relaterte søknader som mottas samtidig havner på samme sak.
- Ingen ny infrastruktur, bare den eksisterende låsmekanismen i saksflyt.
- Konsumenten gjør ikke lenger oppslag i databasen for routing.

**Avveininger**
- En `FEILET` prosess holder gruppen, så relaterte søknader blir stående `PÅ_VENT` til
  den restartes. Det er ønsket, fordi alternativet er en ny sak, men det må overvåkes.
  Antall prosesser `PÅ_VENT` per type eksponeres som metrikk
  (`melosys.prosessinstanser.paa.vent`).
- Manuell ferdigstilling via admin publiserer ikke ferdig-hendelse, så ventende prosesser
  i samme gruppe frigjøres ikke automatisk.
- Under rullerende deploy kan en gammel pod opprette prosessinstanser med det gamle
  typenavnet etter migreringen.
