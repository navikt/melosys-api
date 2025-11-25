# Deferred Prosessinstans Pattern

## Problem: Race Condition ved Opprettelse av Prosessinstanser

Når vi oppretter en prosessinstans i samme transaksjon som vi oppdaterer data, kan det oppstå en race condition hvor prosessen leser data fra databasen før transaksjonen har committet.

### Symptomer

- Kafka-meldinger inneholder gamle/feil verdier
- Prosesser som leser fra database ser `null` eller uoppdaterte verdier
- Problemet er flaky - det skjer oftere i CI enn lokalt
- Debug-logging viser at dataen blir satt riktig, men lesing returnerer feil verdi

### Rotårsak

```
┌─────────────────────────────────────────────────────────────────────────┐
│ @Transactional (VedtaksfattingFasade.fattVedtak)                        │
│                                                                         │
│  1. oppdaterBehandlingsresultat()                                       │
│     └─→ behandlingsresultat.type = MEDLEM_I_FOLKETRYGDEN                │
│     └─→ flush() ← skriver til DB, men IKKE committet ennå!              │
│                                                                         │
│  2. opprettProsessinstansIverksettVedtakFTRL()                          │
│     └─→ prosessinstansRepo.save()                                       │
│     └─→ publishEvent(ProsessinstansOpprettetEvent)                      │
│                                                                         │
│  3. produserOgDistribuerBrev()                                          │
│  4. ferdigstillOppgave()                                                │
│                                                                         │
└──────────────────────────────────────────────── COMMIT skjer her ───────┘
                                                        │
                                                        ▼
                                AFTER_COMMIT listener starter async prosess
                                                        │
                                                        ▼
                                Prosess leser fra DB - kan se gammel data!
```

Selv om `@TransactionalEventListener(phase = AFTER_COMMIT)` brukes, kan det være et mikroskopisk tidsvindu hvor:
- Database-commit er fullført fra Spring sitt perspektiv
- Men den nye async-tråden får en database-connection som ikke har mottatt commit-notifikasjonen ennå

## Løsning: Deferred Prosessinstans Pattern

Flytt opprettelsen av prosessinstansen til ETTER at data-transaksjonen har committet.

### Arkitektur

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Transaksjon A: fattVedtak()                                             │
│  1. Oppdater behandlingsresultat (type, vedtakMetadata)                 │
│  2. Publiser StartProsessinstansEtterCommitEvent                        │
│  3. Andre operasjoner...                                                │
└───────────────────────────────────────────────── COMMIT (data synlig!) ─┘
                                                        │
                                                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ AFTER_COMMIT: StartProsessinstansEtterCommitListener                    │
│  → Opprett prosessinstans (ny transaksjon B)                            │
└───────────────────────────────────────────────── COMMIT ────────────────┘
                                                        │
                                                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ AFTER_COMMIT: ProsessinstansOpprettetListener                           │
│  → Start async prosess                                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                                        │
                                                        ▼
                                Prosess leser fra DB - garantert oppdatert data!
```

### Komponenter

#### 1. Event-klassen (`saksflyt-api`)

```kotlin
// StartProsessinstansEtterCommitEvent.kt
sealed class StartProsessinstansEtterCommitEvent(
    val behandlingId: Long
) : ApplicationEvent(behandlingId) {

    class IverksettVedtakFtrl(
        behandlingId: Long,
        val vedtakRequest: VedtakRequest,
        val saksstatus: Saksstatuser
    ) : StartProsessinstansEtterCommitEvent(behandlingId) {
        override val prosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
    }

    class IverksettVedtakTrygdeavtale(
        behandlingId: Long
    ) : StartProsessinstansEtterCommitEvent(behandlingId) {
        override val prosessType = ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE
    }

    // Legg til flere subklasser etter behov...

    abstract val prosessType: ProsessType
}
```

#### 2. Listener (`saksflyt`)

```kotlin
// StartProsessinstansEtterCommitListener.kt
@Component
class StartProsessinstansEtterCommitListener(
    private val prosessinstansService: ProsessinstansService,
    private val behandlingService: BehandlingService
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun opprettProsessinstansEtterCommit(event: StartProsessinstansEtterCommitEvent) {
        val behandling = behandlingService.hentBehandling(event.behandlingId)

        when (event) {
            is StartProsessinstansEtterCommitEvent.IverksettVedtakFtrl -> {
                prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(
                    behandling, event.vedtakRequest, event.saksstatus
                )
            }
            is StartProsessinstansEtterCommitEvent.IverksettVedtakTrygdeavtale -> {
                prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling)
            }
            // Håndter flere typer...
        }
    }
}
```

## Bruk

### Før (direkte kall - kan ha race condition)

```kotlin
@Service
class MinVedtakService(
    private val prosessinstansService: ProsessinstansService
) {
    fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
        // Oppdater data
        oppdaterBehandlingsresultat(behandling, request)

        // Start prosess - kan lese gammel data!
        prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(
            behandling, request.tilVedtakRequest(), saksstatus
        )
    }
}
```

### Etter (deferred - garantert oppdatert data)

```kotlin
@Service
class MinVedtakService(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
        // Oppdater data
        oppdaterBehandlingsresultat(behandling, request)

        // Publiser event - prosess starter ETTER commit
        applicationEventPublisher.publishEvent(
            StartProsessinstansEtterCommitEvent.IverksettVedtakFtrl(
                behandlingId = behandling.id,
                vedtakRequest = request.tilVedtakRequest(),
                saksstatus = saksstatus
            )
        )
    }
}
```

## Legge til ny prosesstype

1. **Legg til ny subklasse i `StartProsessinstansEtterCommitEvent`:**

```kotlin
class MinNyeProsess(
    behandlingId: Long,
    val ekstraData: String
) : StartProsessinstansEtterCommitEvent(behandlingId) {
    override val prosessType = ProsessType.MIN_NYE_PROSESS
}
```

2. **Håndter den i `StartProsessinstansEtterCommitListener`:**

```kotlin
when (event) {
    // ... eksisterende cases ...
    is StartProsessinstansEtterCommitEvent.MinNyeProsess -> {
        prosessinstansService.opprettProsessinstansMinNyeProsess(
            behandling, event.ekstraData
        )
    }
}
```

3. **Bruk den i servicen din:**

```kotlin
applicationEventPublisher.publishEvent(
    StartProsessinstansEtterCommitEvent.MinNyeProsess(
        behandlingId = behandling.id,
        ekstraData = "verdi"
    )
)
```

## Når skal du bruke dette mønsteret?

### Bruk det når:
- Du oppdaterer data OG starter en prosess som leser denne dataen
- Prosessen kjører asynkront (`@Async`)
- Du har opplevd eller mistenker race conditions

### Ikke nødvendig når:
- Prosessen ikke leser data fra databasen
- All nødvendig data sendes direkte gjennom prosessinstans.data
- Operasjonen er synkron og i samme transaksjon

## Testing

Ved testing av services som bruker dette mønsteret, verifiser at riktig event blir publisert:

```kotlin
@Test
fun `skal publisere event ved vedtakfatning`() {
    // ... setup ...

    service.fattVedtak(behandling, request)

    verify { applicationEventPublisher.publishEvent(capture(eventSlot)) }

    (eventSlot.captured as StartProsessinstansEtterCommitEvent.IverksettVedtakFtrl).run {
        behandlingId shouldBe expectedBehandlingId
        saksstatus shouldBe expectedStatus
    }
}
```

## Relaterte filer

- `saksflyt-api/src/main/kotlin/no/nav/melosys/saksflytapi/StartProsessinstansEtterCommitEvent.kt`
- `saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/StartProsessinstansEtterCommitListener.kt`
- `service/src/main/kotlin/no/nav/melosys/service/vedtak/FtrlVedtakService.kt` (eksempel på bruk)

## Referanser

- [Spring @TransactionalEventListener](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- JIRA: MELOSYS-7718
