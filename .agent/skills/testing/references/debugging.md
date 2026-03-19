# Testing Debugging Guide

## Common Issues

### 1. Oracle Container Won't Start

**Symptom**: Test hangs at container startup

**Cause**: Docker resource limits or ARM Mac incompatibility

**Solution**:
```bash
# Use local Oracle instead
export USE_LOCAL_DB=true

# Ensure local Oracle is running (melosys-docker-compose)
docker-compose up -d oracle
```

### 2. Testcontainer Image Pull Failure

**Symptom**: `ImagePullException` or authentication error

**Cause**: GitHub package auth required

**Solution**:
```bash
# Login to ghcr.io
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

### 3. Embedded Kafka Issues

**Symptom**: `KafkaException` or consumer timeout

**Cause**: Topics not created or partition mismatch

**Debug**:
```kotlin
// Check topic exists
@EmbeddedKafka(topics = ["teammelosys.xxx.v1-local"])

// Verify embedded broker started
println(embeddedKafkaBroker.brokersAsString)
```

### 4. Spring Context Fails to Start

**Symptom**: `ApplicationContextException`

**Cause**: Missing beans, config, or mock setup

**Debug**:
```kotlin
// Check active profile
@ActiveProfiles("test")

// Ensure mocks are imported
@Import(KafkaTestConfig::class, KodeverkTestConfig::class)
```

### 5. Database Cleanup Failure

**Symptom**: Constraint violations or stale data

**Cause**: Cleanup order wrong or missing

**Solution**:
```kotlin
// Add cleanup in correct order (child before parent)
addCleanUpAction {
    deleteBehandling(behandlingId)
    deleteFagsak(fagsakId)
}
```

### 6. Feature Toggle Not Working

**Symptom**: Feature behaves incorrectly in test

**Cause**: FakeUnleash not configured

**Solution**:
```kotlin
@BeforeEach
fun setup() {
    fakeUnleash.enable("MY_FEATURE")
}

@AfterEach
fun cleanup() {
    fakeUnleash.enableAll()
}
```

### 7. Test Flakiness

**Symptom**: Test passes sometimes, fails sometimes

**Cause**: Race conditions, async processing, or shared state

**Solutions**:
```kotlin
// Use explicit waits
await.atMost(Duration.ofSeconds(10)).until {
    condition == true
}

// Use @DirtiesContext for context isolation
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

// Use KafkaOffsetChecker for Kafka
kafkaOffsetChecker.waitForConsumerToCatchUp(topic, groupId, timeout)
```

## Test Execution Commands

### Single Test

```bash
# Unit test
~/.claude/scripts/run-tests.sh -pl service -Dtest=MyServiceTest

# Specific method
~/.claude/scripts/run-tests.sh -pl service -Dtest=MyServiceTest#testMethodName

# Integration test
~/.claude/scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=MyIT
```

### Test Groups

```bash
# All unit tests in module
~/.claude/scripts/run-tests.sh -pl service

# All integration tests
~/.claude/scripts/run-tests.sh -pl integrasjonstest --integration

# With dependency rebuild
~/.claude/scripts/run-tests.sh -pl integrasjonstest -am --integration
```

### Debug Mode

```bash
# Maven debug
mvn -pl service test -Dtest=MyTest -Dmaven.surefire.debug
# Attach debugger to port 5005

# View full logs
cat /tmp/mvn.log
```

## Test Data Setup

### Using E2ETestDataService

```kotlin
@Autowired
private lateinit var e2eTestDataService: E2ETestDataService

val fagsak = e2eTestDataService.opprettFagsak(
    sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
    sakstype = Sakstyper.EU_EOS
)

val behandling = e2eTestDataService.opprettBehandling(
    fagsakId = fagsak.id,
    behandlingstema = Behandlingstema.YRKESAKTIV
)
```

### Manual Test Data

```kotlin
val fagsak = Fagsak().apply {
    saksnummer = "MEL-123"
    sakstype = Sakstyper.EU_EOS
}
fagsakRepository.save(fagsak)

addCleanUpAction { deleteFagsak(fagsak.id) }
```

## Mock Configuration

### MelosysMockContainerConfig

```kotlin
companion object {
    @DynamicPropertySource
    @JvmStatic
    fun configureMockProperties(registry: DynamicPropertyRegistry) {
        MelosysMockContainerConfig.configureProperties(registry)
    }
}
```

### MockVerificationClient

```kotlin
// Verify endpoint was called
mockVerificationClient.verify("/api/endpoint", expectedCallCount = 1)

// Clear mock state
mockVerificationClient.clear()
```

## Kotest Assertions

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

result shouldBe expected
result shouldNotBe null
list shouldContain element
obj.shouldBeInstanceOf<ExpectedType>()
```

## JUnit 5 Assertions

```kotlin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

assertThat(result).isNotNull()
assertThat(list).hasSize(3)
assertThat(obj).isInstanceOf(ExpectedType::class.java)

assertThrows<ExpectedException> {
    service.doSomething()
}
```

## Mockito Usage

```kotlin
import org.mockito.kotlin.*

// Stubbing
whenever(repository.findById(any())).thenReturn(Optional.of(entity))
doReturn(result).whenever(spy).method()

// Verification
verify(repository).save(any())
verify(repository, times(2)).findById(any())
verify(repository, never()).delete(any())

// Argument capture
val captor = argumentCaptor<Entity>()
verify(repository).save(captor.capture())
assertThat(captor.firstValue.name).isEqualTo("expected")
```

## MockK Usage

```kotlin
import io.mockk.*

// Stubbing
every { repository.findById(any()) } returns Optional.of(entity)
coEvery { suspendService.fetch() } returns result

// Verification
verify { repository.save(any()) }
verify(exactly = 2) { repository.findById(any()) }
verify(exactly = 0) { repository.delete(any()) }

// Slot capture
val slot = slot<Entity>()
verify { repository.save(capture(slot)) }
assertThat(slot.captured.name).isEqualTo("expected")
```

## Environment Variables for Tests

```bash
# Use local Oracle
export USE_LOCAL_DB=true

# ARM Mac Oracle image
export ORACLE_IMAGE=ghcr.io/navikt/melosys-legacy-avhengigheter/oracle-arm:19.3.0-ee-slim-faststart
export MELOSYS_ORACLE_DB_NAME=FREEPDB1

# Intel Oracle image (default)
export MELOSYS_ORACLE_DB_NAME=XEPDB1
```

## Related Skills

- **kafka**: Kafka-specific testing
- **database**: Schema for test data
- **saksflyt**: Saga testing patterns
