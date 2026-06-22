# Vedtak Types Reference

## Table of Contents
1. [Vedtak Process Types](#vedtak-process-types)
2. [Service Layer Architecture](#service-layer-architecture)
3. [Step Implementations](#step-implementations)
4. [Behandlingsresultattyper](#behandlingsresultattyper)

## Vedtak Process Types

### IVERKSETT_VEDTAK_FTRL

**Purpose**: Execute Folketrygdloven (National Insurance Act) verdict

**Trigger**: `FtrlVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | LAGRE_MEDLEMSKAPSPERIODE_MEDL | `LagreMedlemsperiodeMedl` | Save membership period to MEDL |
| 3 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 4 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 5 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |
| 6 | OPPRETTE_AARSAVREGNING_ENDRING | `OppretteÅrsavregningVedEndring` | Trigger årsavregning change behandling |
| 7 | RESET_ÅPNE_ÅRSAVREGNINGER | `ResetÅpneÅrsavregningBehandlinger` | Reset open annual reconciliations (side effect after vedtak) |

**Data Keys Used**:
- `VEDTAK_REQUEST` - The vedtak request from frontend
- `NY_SAKSSTATUS` - New case status after verdict

---

### IVERKSETT_VEDTAK_EOS

**Purpose**: Execute EØS (EU/EEA) verdict with foreign authority communication

**Trigger**: `EosVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | AVKLAR_MYNDIGHET | `AvklarMyndighet` | Clarify foreign authority |
| 3 | AVKLAR_ARBEIDSGIVER | `AvklarArbeidsgiver` | Clarify Norwegian employer |
| 4 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 5 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 6 | SEND_VEDTAKSBREV_INNLAND | `SendVedtaksbrevInnland` | Send domestic verdict letter |
| 7 | SEND_VEDTAK_UTLAND | `SendVedtakUtland` | Send SED to foreign authority |
| 8 | DISTRIBUER_JOURNALPOST_UTLAND | `DistribuerJournalpostUtland` | Distribute to foreign journal |
| 9 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 10 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |
| 11 | OPPRETTE_AARSAVREGNING_ENDRING | `OppretteÅrsavregningVedEndring` | Trigger årsavregning change behandling |
| 12 | RESET_ÅPNE_ÅRSAVREGNINGER | `ResetÅpneÅrsavregningBehandlinger` | Reset open annual reconciliations |

---

### IVERKSETT_VEDTAK_TRYGDEAVTALE

**Purpose**: Execute bilateral social security agreement verdict

**Trigger**: `TrygdeavtaleVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | AVKLAR_MYNDIGHET | `AvklarMyndighet` | Clarify foreign authority |
| 3 | AVKLAR_ARBEIDSGIVER | `AvklarArbeidsgiver` | Clarify Norwegian employer |
| 4 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 5 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 6 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_VEDTAK_IKKE_YRKESAKTIV

**Purpose**: Execute non-worker verdict

**Trigger**: Selected inside `EosVedtakService.fattVedtak()` when
`saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)` — there is no dedicated
`IkkeYrkesaktivVedtakService`. It calls `prosessinstansService.opprettProsessinstansIverksettIkkeYrkesaktiv(behandling)`.

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 3 | SEND_VEDTAKSBREV_INNLAND | `SendVedtaksbrevInnland` | Send domestic verdict letter |
| 4 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 5 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_VEDTAK_AARSAVREGNING

**Purpose**: Execute annual reconciliation verdict

**Trigger**: `ÅrsavregningVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | SEND_FAKTURA_AARSAVREGNING | `SendFakturaÅrsavregning` | Send annual invoice |
| 3 | VARSLE_PENSJONSOPPTJENING | `SendPensjonsopptjeningHendelse` | Notify pension accrual |
| 4 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 5 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_EOS_PENSJONIST_AVGIFT

**Purpose**: Execute EØS pensioner fee collection

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_PERSONOPPLYSNINGER | `LagrePersonopplysninger` | Save person data snapshot |
| 2 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 3 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 4 | SEND_ORIENTERINGSBREV_TRYGDEAVGIFT | - | Send orientation letter |
| 5 | OPPRETTE_AARSAVREGNING_ENDRING | `OppretteÅrsavregningVedEndring` | Trigger årsavregning change behandling |
| 6 | RESET_ÅPNE_ÅRSAVREGNINGER | `ResetÅpneÅrsavregningBehandlinger` | Reset open annual reconciliations |

---

## Service Layer Architecture

### VedtaksfattingFasade

Location: `service/src/main/java/no/nav/melosys/service/vedtak/VedtaksfattingFasade.java` (Java; Kotlin-migrering pågår).

Entry point. Validerer at det kan fattes vedtak, og delegerer ruting til `FattVedtakVelger`:

```java
@Transactional(noRollbackFor = {ValideringException.class})
public void fattVedtak(long behandlingID, FattVedtakRequest fattVedtakRequest) throws ValideringException {
    var behandling = behandlingService.hentBehandling(behandlingID);
    validerKanFattesVedtak(behandling);
    FattVedtakInterface fattVedtakInterface = fattVedtakVelger.getFattVedtakService(behandling);
    fattVedtakInterface.fattVedtak(behandling, fattVedtakRequest);
}
```

`FattVedtakVelger.getFattVedtakService(behandling)` (Kotlin) velger tjeneste: behandlingstype
`ÅRSAVREGNING` → `ÅrsavregningVedtakService`, ellers etter sakstype `EU_EOS`/`FTRL`/`TRYGDEAVTALE`.

### VedtakService Implementations

| Sakstype | Service | Location |
|----------|---------|----------|
| FTRL | `FtrlVedtakService` | `service/src/main/kotlin/.../vedtak/FtrlVedtakService.kt` |
| EU_EOS | `EosVedtakService` | `service/src/main/java/.../vedtak/EosVedtakService.java` |
| TRYGDEAVTALE | `TrygdeavtaleVedtakService` | `service/src/main/java/.../vedtak/TrygdeavtaleVedtakService.java` |
| ÅRSAVREGNING (behandlingstype) | `ÅrsavregningVedtakService` | `service/src/main/kotlin/.../vedtak/ÅrsavregningVedtakService.kt` |

`IVERKSETT_VEDTAK_IKKE_YRKESAKTIV` har ingen egen tjeneste — det velges inne i `EosVedtakService`
når `saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)`, som da kaller
`prosessinstansService.opprettProsessinstansIverksettIkkeYrkesaktiv(behandling)`.

### Common VedtakService Pattern

```kotlin
override fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
    // 1. Update behandlingsresultat with vedtak data
    val behandlingsresultat = oppdaterBehandlingsresultat(behandling, request)

    // 2. Check for existing vedtak process (prevent duplicates)
    if (prosessinstansService.harVedtakInstans(behandlingID)) {
        throw FunksjonellException("Det finnes allerede en vedtak-prosess...")
    }

    // 3. Set behandling status to IVERKSETTER_VEDTAK
    behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)

    // 4. Create prosessinstans (triggers async saga)
    prosessinstansService.opprettProsessinstansIverksettVedtak...(behandling, ...)

    // 5. Trigger letter generation (sync)
    dokgenService.produserOgDistribuerBrev(behandlingID, brevbestilling)

    // 6. Close task
    oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingID)
}
```

---

## Behandlingsresultattyper

Verdict result types used in vedtak processing:

| Type | Description | Use Case |
|------|-------------|----------|
| `MEDLEM_I_FOLKETRYGDEN` | Member in National Insurance | Approved FTRL membership |
| `FASTSATT_LOVVALGSLAND` | Established law selection | EØS law selection |
| `OPPHØRT` | Terminated | Full membership termination |
| `DELVIS_OPPHØRT` | Partially terminated | Some periods terminated |
| `AVSLAG_MANGLENDE_OPPL` | Rejected - missing info | Rejection due to missing documents |
| `FASTSATT_TRYGDEAVGIFT` | Established social contribution | Annual reconciliation |

---

## Key File Locations

```
service/src/main/java/no/nav/melosys/service/vedtak/
├── VedtaksfattingFasade.java    # Entry point (validation + delegation)
├── EosVedtakService.java        # EØS verdicts (also routes IVERKSETT_VEDTAK_IKKE_YRKESAKTIV)
├── TrygdeavtaleVedtakService.java # Bilateral agreement verdicts
└── FattVedtakRequest.java       # Request DTO

service/src/main/kotlin/no/nav/melosys/service/vedtak/
├── FattVedtakVelger.kt          # Router: behandling -> FattVedtakInterface
├── FattVedtakInterface.kt       # Common interface
├── FtrlVedtakService.kt         # FTRL verdicts
└── ÅrsavregningVedtakService.kt # Annual reconciliation

saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/steg/
├── medl/                        # MEDL integration steps
├── brev/                        # Letter sending steps
├── fakturering/                 # Invoice steps
├── behandling/                  # Case/treatment steps
└── melding/                     # Kafka message steps
