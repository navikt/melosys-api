# Race Condition Analyse: Sync/Async Split i Vedtaksflyt

## Innhold
1. [Oversikt](#oversikt)
2. [Rotårsak: Sync/Async Split](#rotårsak-syncasync-split)
3. [Identifiserte Race Conditions](#identifiserte-race-conditions)
4. [Implementerte Fixes (PRs)](#implementerte-fixes-prs)
5. [Gjenstående Risiko](#gjenstående-risiko)
6. [Langsiktig Løsning](#langsiktig-løsning)
7. [Taktiske Fixes](#taktiske-fixes)

---

## Oversikt

Melosys-api har en arkitektur der vedtaksfatting (`fattVedtak()`) starter en asynkron saga-prosess. Dette skaper et **sync/async split** der HTTP-tråden og saga-tråden kan modifisere samme entiteter parallelt.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST TRANSACTION                             │
│                                                                              │
│  VedtakService.fattVedtak() {                                               │
│    behandlingsresultat.setType(...)      ─┬─ Modifiserer entiteter          │
│    behandling.setStatus(IVERKSETTER...)   │                                 │
│    prosessinstansService.opprett...()  ───┤─ Starter saga                   │
│    dokgenService.produserBrev()           │                                 │
│    oppgaveService.ferdigstill()          ─┘                                 │
│  }                                                                          │
│                                                                              │
│  Event listeners kjører BEFORE_COMMIT:                                      │
│    • SaksoppplysningEventListener.lagrePersonopplysninger()                 │
│    • FaktureringEventListener.oppdaterFakturaMottaker()                     │
└───────────────────────────────────────┬──────────────────────────────────────┘
                                        │ COMMIT
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ASYNC SAGA EXECUTION (egen tråd)                        │
│                                                                              │
│  Hvert saga-steg kjører i REQUIRES_NEW transaksjon:                         │
│    • LAGRE_PERSONOPPLYSNINGER                                               │
│    • HENT_REGISTEROPPLYSNINGER  ──────── Kan modifisere samme entiteter!    │
│    • OPPRETT_FAKTURASERIE                                                   │
│    • ...                                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Konsekvenser:**
- Silent data corruption (felt overskrives uten feilmelding)
- `OptimisticLockingException`
- Sporadiske feil som er vanskelige å reprodusere

---

## Rotårsak: Sync/Async Split

### Standard JPA UPDATE-oppførsel

Hibernate inkluderer **alle kolonner** i UPDATE-setninger, selv når kun ett felt er endret:

```sql
-- Uten @DynamicUpdate
UPDATE behandlingsresultat
SET resultat_type = ?, vedtak_dato = ?, fritekst = ?, ... (alle 20+ kolonner)
WHERE id = ?
```

Når saga-tråden har en **stale versjon** av entiteten i minnet, overskriver den endringer gjort av HTTP-tråden.

### Race Condition Scenario

```
Tid     HTTP-tråd                           Saga-tråd
────    ─────────                           ──────────
T1      Les Behandlingsresultat
        (type = IKKE_FASTSATT)

T2      type = MEDLEM_I_FOLKETRYGDEN

T3      save() [ikke committed ennå]        Starter (AFTER_COMMIT trigger)

T4                                          Les Behandlingsresultat
                                            (type = IKKE_FASTSATT - stale!)

T5      COMMIT

T6                                          Modifiser annet felt

T7                                          save() → UPDATE alle kolonner
                                            → type = IKKE_FASTSATT overskriver!
```

---

## Identifiserte Race Conditions

### 1. Behandlingsresultat.type Overskrives (MELOSYS-7718)

**Symptom:** Silent data corruption - `type` settes tilbake til `IKKE_FASTSATT`

**Årsak:**
- `FtrlVedtakService.fattVedtak()` setter `type = MEDLEM_I_FOLKETRYGDEN`
- Saga-steg (f.eks. `OpprettFakturaserie`) laster andre behandlingers `Behandlingsresultat`
- Ved flush overskriver saga stale data

**Berørte services:**
- `FtrlVedtakService`
- `ÅrsavregningVedtakService`

### 2. SaksopplysningKilde Konflikt (MELOSYS-7754)

**Symptom:** `OptimisticLockingException: Row was updated or deleted by another transaction`

**Årsak:**
- `SaksoppplysningEventListener` kjører synkront på `IVERKSETTER_VEDTAK`
- Saga-steg `HentRegisteropplysninger` modifiserer samme `saksopplysninger`
- Hibernate oppdager versjonskonflikten

**Berørte entiteter:**
- `Saksopplysning`
- `SaksopplysningKilde`

### 3. Behandling med Stale OppgaveId (MELOSYS-7803)

**Symptom:** `ObjectOptimisticLockingFailureException` ved lagring av oppgaveId

**Årsak:**
- `OppgaveService.settOppgaveIdPåBehandling()` bruker et `Behandling`-objekt lastet i tidligere transaksjon
- Saga har oppdatert samme behandling i mellomtiden
- Versjonsnummer matcher ikke ved lagring

---

## Implementerte Fixes (PRs)

### PR #3160 - MELOSYS-7718: @DynamicUpdate + saveAndFlush()

**Type:** Taktisk fix

**Endringer:**
```kotlin
// domain/src/main/kotlin/.../Behandlingsresultat.kt
@Entity
@DynamicUpdate  // <-- Kun endrede kolonner i UPDATE
class Behandlingsresultat { ... }

// service/.../FtrlVedtakService.kt
behandlingsresultatService.lagreOgFlush(behandlingsresultat)  // <-- Flush umiddelbart
```

**Hvordan det hjelper:**
- `@DynamicUpdate`: Saga-steg som kun endrer `lovvalgsperioder` vil ikke overskrive `type`
- `saveAndFlush()`: Sikrer at HTTP-trådens endringer er i DB før saga starter

### PR #3161 - MELOSYS-7754: Flytt til Saga Step

**Type:** Arkitekturfix

**Endringer:**
- Nytt saga step: `LAGRE_PERSONOPPLYSNINGER`
- Fjernet `IVERKSETTER_VEDTAK` fra `SaksoppplysningEventListener`
- Event listener håndterer fortsatt `AVSLUTTET` og `MIDLERTIDIG_LOVVALGSBESLUTNING`

**Berørte prosessflyter:**
- `IVERKSETT_VEDTAK_AARSAVREGNING`
- `IVERKSETT_VEDTAK_EOS`
- `IVERKSETT_EOS_PENSJONIST_AVGIFT`
- `IVERKSETT_VEDTAK_FTRL`
- `IVERKSETT_VEDTAK_IKKE_YRKESAKTIV`
- `IVERKSETT_VEDTAK_TRYGDEAVTALE`

### PR #3166 - MELOSYS-7803: Reload Behandling

**Type:** Taktisk fix

**Endringer:**
```kotlin
// service/.../OppgaveService.kt
private fun settOppgaveIdPåBehandling(behandling: Behandling, oppgaveId: String) {
    // Last på nytt for å unngå stale object
    val freshBehandling = behandlingService.hentBehandling(behandling.id)
    freshBehandling.oppgaveId = oppgaveId
    behandlingService.lagre(freshBehandling)
}
```

---

## Gjenstående Risiko

### 1. VedtakService Modifiserer Fortsatt Entiteter Synkront

| Service | Sync operasjoner |
|---------|------------------|
| `FtrlVedtakService` | `oppdaterBehandlingsresultat()`, `produserBrev()`, `ferdigstillOppgave()` |
| `EosVedtakService` | `oppdaterBehandlingsresultat()`, `ferdigstillOppgave()` |
| `TrygdeavtaleVedtakService` | `oppdaterBehandlingsresultat()`, `produserBrev()`, `ferdigstillOppgave()` |
| `ÅrsavregningVedtakService` | `oppdaterBehandlingsresultat()` |

**Risiko:** `@DynamicUpdate` beskytter kun når ulike felter modifiseres. Hvis saga-steg modifiserer samme felt, kan det fortsatt oppstå konflikter.

### 2. SaksopplysningEventListener for AVSLUTTET

```java
// Fortsatt aktiv for disse statusene:
if (List.of(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
    .contains(event.getBehandlingsstatus())) {
    personopplysningerLagrer.lagreHvisMangler(event.getBehandling().getId());
}
```

**Risiko:** Hvis saga-steg også kjører ved disse statusene, kan samme race condition oppstå.

### 3. Manglende @DynamicUpdate på Flere Entiteter

Entiteter som kan være utsatt:
- `Saksopplysning`
- `SaksopplysningKilde`
- `Behandling`
- Andre entiteter som modifiseres av både sync og async kode

---

## Langsiktig Løsning

### Prinsipp: Alt i Saga

Flytt **all** entitetsmodifisering fra synkrone VedtakService-metoder til saga-steg:

```
FØR (problematisk):                     ETTER (trygt):
─────────────────────                   ──────────────────
HTTP-tråd:                              HTTP-tråd:
  ├─ Modifiser entiteter                  ├─ Validering
  ├─ Start saga                           └─ Start saga
  └─ Mer modifisering
                                        Saga-tråd (sekvensielt):
Saga-tråd (parallelt):                    ├─ OPPDATER_BEHANDLINGSRESULTAT
  └─ Modifiser entiteter                  ├─ LAGRE_PERSONOPPLYSNINGER
         ↓                                ├─ SEND_BREV
    RACE CONDITION                        ├─ FERDIGSTILL_OPPGAVE
                                          └─ ...
```

### Nye Saga Steps

1. **OPPDATER_BEHANDLINGSRESULTAT_VEDTAK**
   - Flytt `oppdaterBehandlingsresultat()` fra alle VedtakServices
   - Første steg i alle IVERKSETT_VEDTAK-flyter

2. **FERDIGSTILL_OPPGAVE_VEDTAK**
   - Flytt `oppgaveService.ferdigstillOppgave()` fra alle VedtakServices
   - Kan integreres i `AVSLUTT_SAK_OG_BEHANDLING` eller eget steg

### Refaktorert VedtakService

```kotlin
// FtrlVedtakService.fattVedtak() - kun validering + start saga
override fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
    validerRequest(behandling, request)

    if (prosessinstansService.harVedtakInstans(behandling.id)) {
        throw FunksjonellException("Det finnes allerede en vedtak-prosess")
    }

    behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)

    // Alt annet skjer i saga
    prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(
        behandling,
        request.tilVedtakData()  // Data sendes med til saga
    )
}
```

---

## Taktiske Fixes

Hvis arkitekturfiksene ikke kan gjøres umiddelbart, kan følgende taktiske fixes redusere risiko:

### 1. @DynamicUpdate på Flere Entiteter

```kotlin
@Entity
@DynamicUpdate
class Saksopplysning { ... }

@Entity
@DynamicUpdate
class SaksopplysningKilde { ... }

@Entity
@DynamicUpdate
class Behandling { ... }
```

### 2. saveAndFlush() Før Saga Start

```kotlin
// I alle VedtakServices
behandlingsresultatService.lagreOgFlush(behandlingsresultat)
behandlingService.lagreOgFlush(behandling)
prosessinstansService.opprettProsessinstans...()  // Etter flush
```

### 3. Reload Entiteter i Saga Steps

```kotlin
// I saga steps som modifiserer entiteter lastet av andre
override fun utfør(prosessinstans: Prosessinstans) {
    val behandling = behandlingService.hentBehandling(prosessinstans.behandling.id)
    // Bruk fresh entity
}
```

---

## Referanser

- [Anti-Patterns Dokumentasjon](../.claude/skills/saksflyt/references/anti-patterns.md)
- [Saksflyt Arkitektur](../.claude/skills/saksflyt/references/architecture.md)
- PR #3160: https://github.com/navikt/melosys-api/pull/3160
- PR #3161: https://github.com/navikt/melosys-api/pull/3161
- PR #3166: https://github.com/navikt/melosys-api/pull/3166

---

*Sist oppdatert: 2025-01-08*
