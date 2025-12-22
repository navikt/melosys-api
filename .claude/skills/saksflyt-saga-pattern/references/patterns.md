# Common Patterns

## Table of Contents
1. [Adding a New Step](#adding-a-new-step)
2. [Adding a New Process Type](#adding-a-new-process-type)
3. [Accessing Data in Steps](#accessing-data-in-steps)
4. [Error Handling](#error-handling)
5. [Testing Sagas](#testing-sagas)
6. [Debugging Failed Sagas](#debugging-failed-sagas)

## Adding a New Step

### 1. Define the Step Enum

Add to `saksflyt-api/src/main/java/.../domain/ProsessSteg.java`:

```java
public enum ProsessSteg {
    // ... existing steps ...
    MY_NEW_STEP("MY_NEW_STEP", "Description of what the step does"),
    // ...
}
```

### 2. Implement the Step Handler

Create in `saksflyt/src/main/java/.../steg/{category}/MyNewStep.java`:

```java
package no.nav.melosys.saksflyt.steg.{category};

import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.stereotype.Component;

@Component
public class MyNewStep implements StegBehandler {

    private final SomeService someService;

    public MyNewStep(SomeService someService) {
        this.someService = someService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MY_NEW_STEP;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        // Get data from prosessinstans
        String someValue = prosessinstans.getData(ProsessDataKey.SOME_KEY);
        Behandling behandling = prosessinstans.getBehandling();

        // Do the work
        someService.doSomething(behandling, someValue);

        // Optionally store results for later steps
        prosessinstans.setData(ProsessDataKey.RESULT_KEY, "result");
    }
}
```

### 3. Add to Flow Definition

Edit `saksflyt/src/main/kotlin/.../prosessflyt/ProsessflytDefinisjon.kt`:

```kotlin
SOME_PROCESS_TYPE to ProsessFlyt(
    prosessType = SOME_PROCESS_TYPE,
    EXISTING_STEP_1,
    MY_NEW_STEP,  // Add your step in the right position
    EXISTING_STEP_2
),
```

### 4. Write Tests

```kotlin
// Unit test for the step
@Test
fun `utfør - should call service with correct parameters`() {
    val prosessinstans = lagProsessinstans {
        type = ProsessType.SOME_TYPE
        behandling = testBehandling
        setData(ProsessDataKey.SOME_KEY, "value")
    }

    myNewStep.utfør(prosessinstans)

    verify(someService).doSomething(testBehandling, "value")
}
```

## Adding a New Process Type

### 1. Define the Process Type

Add to `saksflyt-api/src/main/java/.../domain/ProsessType.java`:

```java
public enum ProsessType {
    // ... existing types ...
    MY_NEW_PROCESS("MY_NEW_PROCESS", "Description of the process"),
    // ...
}
```

### 2. Define the Flow

Add to `ProsessflytDefinisjon.kt`:

```kotlin
MY_NEW_PROCESS to ProsessFlyt(
    prosessType = MY_NEW_PROCESS,
    STEP_1,
    STEP_2,
    STEP_3,
    AVSLUTT_SAK_OG_BEHANDLING  // Often ends with this
),
```

### 3. Create Factory Method

Add to `ProsessinstansService.java`:

```java
public UUID opprettProsessinstansMyNewProcess(Behandling behandling, String extraData) {
    Prosessinstans prosessinstans = Prosessinstans.builder()
        .medType(ProsessType.MY_NEW_PROCESS)
        .medBehandling(behandling)
        .medData(ProsessDataKey.EXTRA_DATA, extraData)
        .build();

    return lagre(prosessinstans);
}
```

## Accessing Data in Steps

### Reading Data

```java
// Simple string
String value = prosessinstans.getData(ProsessDataKey.SOME_KEY);

// With type conversion
Boolean flag = prosessinstans.getData(ProsessDataKey.FLAG, Boolean.class);

// With default value
Boolean flag = prosessinstans.getData(ProsessDataKey.FLAG, Boolean.class, false);

// Complex object
MelosysEessiMelding melding = prosessinstans.getData(
    ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

// Using TypeReference for generics
List<String> items = prosessinstans.getData(
    ProsessDataKey.ITEMS, new TypeReference<List<String>>() {});
```

### Kotlin Extensions

```kotlin
// Get required value (throws if missing)
val value: String = prosessinstans.hentData(ProsessDataKey.SOME_KEY)

// Get optional value with default
val flag: Boolean = prosessinstans.finnData(ProsessDataKey.FLAG, false)

// Get optional value (nullable)
val optional: String? = prosessinstans.finnData(ProsessDataKey.SOME_KEY)
```

### Writing Data

```java
// String value
prosessinstans.setData(ProsessDataKey.SOME_KEY, "value");

// Complex object (JSON serialized)
prosessinstans.setData(ProsessDataKey.RESULT, resultObject);
```

### Common Data Keys

| Key | Type | Description |
|-----|------|-------------|
| `BEHANDLING_ID` | Long | Treatment ID |
| `JOURNALPOST_ID` | String | Journal entry ID |
| `SAKSBEHANDLER` | String | User ID |
| `EESSI_MELDING` | MelosysEessiMelding | EESSI message data |
| `AKTØR_ID` | String | Person actor ID |
| `OPPGAVE_ID` | String | Task ID in Gosys |
| `CORRELATION_ID_SAKSFLYT` | String | Correlation ID for tracing |

## Error Handling

### Automatic Error Capture

Exceptions thrown from steps are automatically captured:

```java
// In ProsessinstansBehandler
private void behandleFeil(Prosessinstans prosessinstans, ProsessSteg steg, Exception e) {
    log.error("Feil ved behandling av prosessinstans {} på steg {}", prosessinstans.getId(), steg, e);
    prosessinstans.leggTilHendelse(steg, e);  // Records stack trace
    prosessinstans.setStatus(ProsessStatus.FEILET);
    lagreProsessinstans(prosessinstans);
}
```

### Custom Error Handling in Steps

```java
@Override
public void utfør(Prosessinstans prosessinstans) {
    try {
        externalService.call();
    } catch (TransientException e) {
        // For transient errors, let it fail and be retried
        throw e;
    } catch (BusinessException e) {
        // For business errors, handle gracefully
        log.warn("Business error in step: {}", e.getMessage());
        prosessinstans.setData(ProsessDataKey.ERROR_MESSAGE, e.getMessage());
        // Continue to next step - don't throw
    }
}
```

### Retry Pattern

Failed sagas can be restarted from last completed step:

```java
// In ProsessinstansAdminService
public void restartProsessinstans(UUID id) {
    Prosessinstans prosessinstans = repository.findById(id).orElseThrow();
    if (!prosessinstans.erFeilet()) {
        throw new IllegalStateException("Can only restart failed sagas");
    }
    prosessinstans.setStatus(ProsessStatus.RESTARTET);
    repository.save(prosessinstans);
    applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
}
```

## Testing Sagas

### Unit Testing Steps

```kotlin
@ExtendWith(MockitoExtension::class)
class MyStepTest {
    @Mock lateinit var someService: SomeService
    @InjectMocks lateinit var myStep: MyStep

    @Test
    fun `should process correctly`() {
        val prosessinstans = lagProsessinstans {
            type = ProsessType.SOME_TYPE
            behandling = testBehandling
        }

        myStep.utfør(prosessinstans)

        verify(someService).doWork(testBehandling)
    }
}

// Helper function
fun lagProsessinstans(block: Prosessinstans.Builder.() -> Unit): Prosessinstans =
    Prosessinstans.builder().apply(block).build()
```

### Integration Testing Flows

```kotlin
@SpringBootTest
@Transactional
class MyFlowIT {
    @Autowired lateinit var prosessinstansService: ProsessinstansService
    @Autowired lateinit var prosessinstansRepository: ProsessinstansRepository

    @Test
    fun `full flow should complete`() {
        // Create saga
        val id = prosessinstansService.opprettMyProcess(behandling)

        // Wait for async completion
        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val prosessinstans = prosessinstansRepository.findById(id).orElseThrow()
            assertThat(prosessinstans.status).isEqualTo(ProsessStatus.FERDIG)
        }

        // Verify side effects
        verify(externalService).wasCalledWith(expected)
    }
}
```

### Testing Concurrency

```kotlin
@Test
fun `concurrent sagas with same lock should wait`() {
    // Create first saga
    val id1 = prosessinstansService.opprettSedMottak(sedMelding1)

    // Create second with same RINA case
    val id2 = prosessinstansService.opprettSedMottak(sedMelding2)

    // Second should be waiting
    val prosessinstans2 = prosessinstansRepository.findById(id2).orElseThrow()
    assertThat(prosessinstans2.status).isEqualTo(ProsessStatus.PÅ_VENT)
}
```

## Debugging Failed Sagas

### Finding Failed Sagas

```sql
SELECT id, type, status, sist_fullfort_steg, registrert_dato, endret_dato
FROM prosessinstans
WHERE status = 'FEILET'
ORDER BY endret_dato DESC;
```

### Viewing Error Details

```sql
SELECT pi.id, pi.type, ph.steg, ph.exception_type, ph.stack_trace
FROM prosessinstans pi
JOIN prosessinstans_hendelse ph ON pi.uuid = ph.prosessinstans_uuid
WHERE pi.status = 'FEILET'
ORDER BY ph.tidspunkt DESC;
```

### Inspecting Saga Data

```sql
SELECT id, type, data
FROM prosessinstans
WHERE id = 'your-uuid-here';
-- Data is JSON in CLOB column
```

### Admin API Endpoints

```bash
# List failed sagas
GET /api/admin/prosessinstanser?status=FEILET

# Get saga details
GET /api/admin/prosessinstanser/{id}

# Restart failed saga
POST /api/admin/prosessinstanser/{id}/restart
```

### Common Failure Patterns

| Error | Likely Cause | Solution |
|-------|--------------|----------|
| `NoSuchElementException: Finner ingen stegbehandler` | Missing step implementation | Add `@Component` to step class |
| `EntityNotFoundException` | Behandling not found | Check behandling_id in data |
| `OptimisticLockException` | Concurrent modification | Check for race condition |
| `WebServiceException` | External service down | Retry or check service health |
| `ConstraintViolationException` | Invalid data for DB | Check data integrity |
