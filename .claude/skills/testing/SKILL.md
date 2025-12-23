---
name: testing
description: |
  Expert knowledge of testing patterns and strategies in melosys-api.
  Use when: (1) Writing unit tests with JUnit 5 or Kotest,
  (2) Setting up integration tests with Testcontainers,
  (3) Understanding the test base classes and hierarchy,
  (4) Using ArchUnit for architecture tests,
  (5) Configuring embedded Kafka for testing,
  (6) Understanding mock patterns and test utilities.
---

# Testing Skill

Expert knowledge of testing patterns and strategies in melosys-api.

## Quick Reference

### Test Types

| Type | Framework | Location | Suffix |
|------|-----------|----------|--------|
| Unit tests | JUnit 5, Kotest | `*/src/test/` | `Test.kt` |
| Integration tests | Spring Boot Test | `integrasjonstest/` | `IT.kt` |
| Architecture tests | ArchUnit | `arkitektur/` | `IT.kt` |

### Test Commands

```bash
# Unit tests for a module
~/.claude/scripts/run-tests.sh -pl service -Dtest=MyServiceTest

# Integration tests without dependency rebuild
~/.claude/scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=MyIT

# Integration tests with dependency rebuild
~/.claude/scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=MyIT
```

### Key Base Classes

| Class | Purpose | Key Features |
|-------|---------|--------------|
| `OracleTestContainerBase` | Oracle DB container | Testcontainers, cleanup |
| `ComponentTestBase` | Full component tests | EmbeddedKafka, MockOAuth2 |
| `DataJpaTestBase` | JPA-only tests | Lighter setup |
| `MockServerTestBaseWithProsessManager` | Saga testing | ProsessManager |
| `AvgiftFaktureringTestBase` | Avgift tests | Billing setup |

## Test Base Class Hierarchy

```
OracleTestContainerBase
    └── ComponentTestBase (@SpringBootTest)
            ├── MockServerTestBaseWithProsessManager
            ├── AvgiftFaktureringTestBase
            └── JournalfoeringBase
```

## OracleTestContainerBase

Base class for tests needing Oracle database.

```kotlin
open class OracleTestContainerBase {
    @Autowired
    var dbCleanup: DBCleanup? = null

    protected fun addCleanUpAction(deleteAction: DBCleanup.() -> Unit) {
        dbCleanUpActions.add { dbCleanup?.deleteAction() }
    }

    companion object {
        val oracleContainer = OracleContainer(
            DockerImageName.parse("ghcr.io/navikt/.../oracle-xe:18.4.0-slim")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe")
        )

        @DynamicPropertySource
        @JvmStatic
        fun oracleProperties(registry: DynamicPropertyRegistry) {
            if (useTestContainer()) {
                registry.add("spring.datasource.url") { oracleContainer.jdbcUrl }
                oracleContainer.start()
            }
        }
    }
}
```

### Local DB Mode

For ARM Macs or faster tests:
```bash
export USE_LOCAL_DB=true
```

## ComponentTestBase

Full component test with all dependencies mocked.

```kotlin
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", ...]
)
@Import(KafkaTestConfig::class, KodeverkTestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
abstract class ComponentTestBase : OracleTestContainerBase()
```

### Unleash (Feature Toggles)

```kotlin
val fakeUnleash: FakeUnleash by lazy {
    unleash.shouldBeInstanceOf<FakeUnleash>()
}

// Enable/disable features
fakeUnleash.enable("MY_FEATURE")
fakeUnleash.disable("MY_FEATURE")
```

### Mock Verification

```kotlin
protected val mockVerificationClient: MockVerificationClient by lazy {
    MockVerificationClient(MelosysMockContainerConfig.getBaseUrl())
}

// Verify mock calls
mockVerificationClient.verify("/api/endpoint", 1)
```

## ArchUnit Tests

Architecture tests enforcing layered architecture.

```kotlin
@AnalyzeClasses(packages = ["no.nav.melosys"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArkitekturTestIT {

    @ArchTest
    val `Melosys has layered architecture`: ArchRule = layeredArchitecture()
        .consideringOnlyDependenciesInAnyPackage("no.nav.melosys")
        .layer("Controller").definedBy("..tjenester.gui..")
        .layer("Service").definedBy("..service..")
        .layer("Integrations").definedBy("..integrasjon..")
        .layer("Persistence").definedBy("..repository..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
}
```

## Embedded Kafka Testing

### Test Configuration

```kotlin
@TestConfiguration
class KafkaTestConfig {
    @Bean
    @Qualifier("melosysEessiMelding")
    fun melosysEessiMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, MelosysEessiMelding> {
        val props = kafkaProperties.buildConsumerProperties(null)
        val producerFactory = DefaultKafkaProducerFactory(
            props, StringSerializer(), JsonSerializer(objectMapper)
        )
        return KafkaTemplate(producerFactory)
    }
}
```

### Sending Test Messages

```kotlin
@Autowired
@Qualifier("melosysEessiMelding")
private lateinit var kafkaTemplate: KafkaTemplate<String, MelosysEessiMelding>

fun sendTestMessage() {
    kafkaTemplate.send("teammelosys.eessi.v1-local", message)
}
```

### Consuming Test Messages

```kotlin
@Component
class MelosysHendelseKafkaConsumer {
    private val records: BlockingQueue<ConsumerRecord<String, MelosysHendelse>> = LinkedBlockingQueue()

    @KafkaListener(topics = ["\${kafka.aiven.melosys-hendelser.topic}"])
    private fun melosysHendelseListener(record: ConsumerRecord<String, MelosysHendelse>) {
        records.add(record)
    }

    fun waitForMessage(timeout: Duration): MelosysHendelse? =
        records.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)?.value()
}
```

## Unit Test Patterns

### JUnit 5 Pattern

```kotlin
@ExtendWith(MockitoExtension::class)
class MyServiceTest {

    @Mock
    private lateinit var repository: MyRepository

    @InjectMocks
    private lateinit var service: MyService

    @Test
    fun `should do something`() {
        // Given
        whenever(repository.findById(any())).thenReturn(Optional.of(entity))

        // When
        val result = service.doSomething(id)

        // Then
        assertThat(result).isNotNull()
        verify(repository).findById(id)
    }
}
```

### Kotest Pattern

```kotlin
class MyServiceTest : FunSpec({

    val repository = mockk<MyRepository>()
    val service = MyService(repository)

    test("should do something") {
        // Given
        every { repository.findById(any()) } returns Optional.of(entity)

        // When
        val result = service.doSomething(id)

        // Then
        result shouldNotBe null
        verify { repository.findById(id) }
    }
})
```

## Integration Test Pattern

```kotlin
class MyServiceIT : ComponentTestBase() {

    @Autowired
    private lateinit var service: MyService

    @Test
    fun `should integrate with database`() {
        // Setup cleanup
        addCleanUpAction { deleteBehandling(behandlingId) }

        // Given
        val fagsak = createTestFagsak()

        // When
        val result = service.process(fagsak.id)

        // Then
        assertThat(result).isNotNull()
    }
}
```

## Test Utilities

### DBCleanup

```kotlin
@Autowired
var dbCleanup: DBCleanup? = null

addCleanUpAction {
    deleteBehandling(behandlingId)
    deleteFagsak(fagsakId)
}
```

### Test Data Builders

```kotlin
// E2ETestDataService in saksflyt module
val fagsak = e2eTestDataService.opprettFagsak(
    sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
    sakstype = Sakstyper.EU_EOS
)
```

### KafkaOffsetChecker

```kotlin
kafkaOffsetChecker.waitForConsumerToCatchUp(
    topic = "teammelosys.xxx",
    groupId = "test-group",
    timeout = Duration.ofSeconds(30)
)
```

## Test Profiles

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/XEPDB1
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}

melosys:
  mock-enabled: true
```

## Debugging Tests

### Run Single Test

```bash
~/.claude/scripts/run-tests.sh -pl service -Dtest=MyServiceTest#testMethodName
```

### Debug Mode

```bash
mvn -pl integrasjonstest test -Dtest=MyIT -Dmaven.surefire.debug
# Then attach debugger to port 5005
```

### View Full Logs

```bash
cat /tmp/mvn.log
```

## Related Skills

- **kafka**: Kafka testing patterns
- **database**: Database schema for test data
- **saksflyt**: Saga testing patterns
