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
| 1 | LAGRE_MEDLEMSKAPSPERIODE_MEDL | `LagreMedlemsperiodeMedl` | Save membership period to MEDL |
| 2 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 3 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 4 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |
| 5 | RESET_ÅPNE_ÅRSAVREGNINGER | `ResetÅpneÅrsavregninger` | Reset open annual reconciliations |

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
| 1 | AVKLAR_MYNDIGHET | `AvklarMyndighet` | Clarify foreign authority |
| 2 | AVKLAR_ARBEIDSGIVER | `AvklarArbeidsgiver` | Clarify Norwegian employer |
| 3 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 4 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 5 | SEND_VEDTAKSBREV_INNLAND | `SendVedtaksbrevInnland` | Send domestic verdict letter |
| 6 | SEND_VEDTAK_UTLAND | `SendVedtakUtland` | Send SED to foreign authority |
| 7 | DISTRIBUER_JOURNALPOST_UTLAND | `DistribuerJournalpostUtland` | Distribute to foreign journal |
| 8 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 9 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_VEDTAK_TRYGDEAVTALE

**Purpose**: Execute bilateral social security agreement verdict

**Trigger**: `TrygdeavtaleVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | AVKLAR_MYNDIGHET | `AvklarMyndighet` | Clarify foreign authority |
| 2 | AVKLAR_ARBEIDSGIVER | `AvklarArbeidsgiver` | Clarify Norwegian employer |
| 3 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 4 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 5 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_VEDTAK_IKKE_YRKESAKTIV

**Purpose**: Execute non-worker verdict

**Trigger**: `IkkeYrkesaktivVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | LAGRE_LOVVALGSPERIODE_MEDL | `LagreLovvalgsperiodeMedl` | Save law selection period |
| 2 | SEND_VEDTAKSBREV_INNLAND | `SendVedtaksbrevInnland` | Send domestic verdict letter |
| 3 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 4 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_VEDTAK_AARSAVREGNING

**Purpose**: Execute annual reconciliation verdict

**Trigger**: `ÅrsavregningVedtakService.fattVedtak()`

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | SEND_FAKTURA_AARSAVREGNING | `SendFakturaÅrsavregning` | Send annual invoice |
| 2 | VARSLE_PENSJONSOPPTJENING | `SendPensjonsopptjeningHendelse` | Notify pension accrual |
| 3 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 4 | SEND_MELDING_OM_VEDTAK | `SendMeldingOmVedtak` | Publish Kafka event |

---

### IVERKSETT_EOS_PENSJONIST_AVGIFT

**Purpose**: Execute EØS pensioner fee collection

**Steps**:

| Order | Step | Implementation | Description |
|-------|------|----------------|-------------|
| 1 | OPPRETT_FAKTURASERIE | `OpprettFakturaserie` | Create invoice series |
| 2 | AVSLUTT_SAK_OG_BEHANDLING | `AvsluttFagsakOgBehandling` | Close case and treatment |
| 3 | SEND_ORIENTERINGSBREV_TRYGDEAVGIFT | - | Send orientation letter |

---

## Service Layer Architecture

### VedtaksfattingFasade

Location: `service/src/main/kotlin/.../vedtak/VedtaksfattingFasade.kt`

Entry point that routes to correct VedtakService based on sakstype:

```kotlin
fun fattVedtak(behandlingID: Long, request: FattVedtakRequest) {
    val behandling = behandlingService.hentBehandling(behandlingID)
    val vedtakService = finnVedtakService(behandling.fagsak.type)
    vedtakService.fattVedtak(behandling, request)
}
```

### VedtakService Implementations

| Sakstype | Service | Location |
|----------|---------|----------|
| FTRL | `FtrlVedtakService` | `service/src/main/kotlin/.../vedtak/` |
| EOS | `EosVedtakService` | `service/src/main/kotlin/.../vedtak/` |
| TRYGDEAVTALE | `TrygdeavtaleVedtakService` | `service/src/main/kotlin/.../vedtak/` |
| AARSAVREGNING | `ÅrsavregningVedtakService` | `service/src/main/kotlin/.../vedtak/` |

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
service/src/main/kotlin/no/nav/melosys/service/vedtak/
├── VedtaksfattingFasade.kt      # Entry point/router
├── FtrlVedtakService.kt         # FTRL verdicts
├── EosVedtakService.kt          # EØS verdicts
├── TrygdeavtaleVedtakService.kt # Bilateral agreement verdicts
├── ÅrsavregningVedtakService.kt # Annual reconciliation
├── FattVedtakRequest.kt         # Request DTO
└── FattVedtakInterface.kt       # Common interface

saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/steg/
├── medl/                        # MEDL integration steps
├── brev/                        # Letter sending steps
├── fakturering/                 # Invoice steps
├── behandling/                  # Case/treatment steps
└── melding/                     # Kafka message steps
