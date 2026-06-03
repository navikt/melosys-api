---
name: testing
description: |
  Expert knowledge of testing patterns and strategies in melosys-api.
  Use when: (1) Writing unit tests with JUnit 5 or Kotest,
  (2) Setting up integration tests with Testcontainers,
  (3) Understanding the test base classes and hierarchy,
  (4) Using ArchUnit for architecture tests,
  (5) Configuring embedded Kafka for testing,
  (6) Understanding mock patterns and test utilities,
  (7) Creating test data with the forTest DSL.
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

`*Test.kt` classes can be plain Spring-free unit tests — they may still use the `forTest` DSL to build in-memory domain objects (no DB, no `truncateAllTables`). Only `*IT.kt` boot Spring and require the Testcontainers Oracle DB.

### Test Commands

```bash
# Unit tests for a module
scripts/run-tests.sh -pl service -Dtest=MyServiceTest

# Integration tests without dependency rebuild
scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=MyIT

# Integration tests with dependency rebuild
scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=MyIT
```

### Key Base Classes

| Class | Purpose | Key Features |
|-------|---------|--------------|
| `OracleTestContainerBase` | Oracle DB container | Testcontainers, `@BeforeEach truncateAllTables()` |
| `ComponentTestBase` | Full component tests | EmbeddedKafka, MockOAuth2 |
| `DataJpaTestBase` | JPA-only tests | Lighter setup |
| `MockServerTestBaseWithProsessManager` | Saga testing + WireMock | ProsessManager (most ITs extend this) |
| `JournalfoeringBase` | Journalføring flows | JournalfoeringService, OppgaveService |
| `AvgiftFaktureringTestBase` | Avgift tests | Billing setup |

## Test Base Class Hierarchy

The chain is linear — each class extends the one above it:

```
OracleTestContainerBase
    └── ComponentTestBase (@SpringBootTest)
            └── MockServerTestBaseWithProsessManager (+ WireMock, ProsessManager)
                    └── JournalfoeringBase
                            └── AvgiftFaktureringTestBase
```

Most integration tests extend `MockServerTestBaseWithProsessManager`, not `ComponentTestBase` directly. `DataJpaTestBase` is a separate, lighter JPA-only base for repository tests.

## OracleTestContainerBase

Base class for tests needing Oracle database. Every integration test gets a **clean database automatically**: a `@BeforeEach truncateAllTables()` disables all foreign-key constraints, `TRUNCATE`s an explicit list of test-data tables (kodeverk/static tables are left alone), then re-enables the constraints. There is no per-test cleanup-registration API — tests do not need to register anything to clean up after themselves.

```kotlin
open class OracleTestContainerBase {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /**
     * Tømmer alle tabeller før hver test for å sikre isolasjon.
     * BeforeEach (ikke AfterEach) gjør at man kan inspisere DB etter en test for feilsøking.
     */
    @BeforeEach
    fun truncateAllTables() {
        val tablesToTruncate = listOf(
            "PROSESSINSTANS_HENDELSER", "PROSESSINSTANS",  // Saksflyt først pga FK
            "AARSAVREGNING", "TRYGDEAVGIFTSPERIODE", "MEDLEMSKAPSPERIODE", "LOVVALG_PERIODE",
            // ... eksplisitt liste, child før parent ...
            "BEHANDLINGSRESULTAT", "BEHANDLING", "AKTOER", "FAGSAK"
        )
        // Deaktiver FK-constraints, TRUNCATE hver tabell, reaktiver constraints (via jdbcTemplate)
    }

    companion object {
        val oracleContainer = OracleContainer(
            DockerImageName.parse("ghcr.io/navikt/melosys-legacy-avhengigheter/oracle-xe:18.4.0-slim")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe")
        )

        @DynamicPropertySource
        @JvmStatic
        fun oracleProperties(registry: DynamicPropertyRegistry) {
            if (useTestContainer()) {
                registry.add("spring.datasource.url") { oracleContainer.jdbcUrl }
                registry.add("spring.datasource.username") { oracleContainer.username }
                registry.add("spring.datasource.password") { oracleContainer.password }
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

`ComponentTestBase` exposes a `mockVerificationClient` and calls `clear()` in `@BeforeEach`, so each test starts with empty mock state. The client exposes domain queries — there is no generic `verify(path, count)` method.

```kotlin
protected val mockVerificationClient: MockVerificationClient by lazy {
    MockVerificationClient(MelosysMockContainerConfig.getBaseUrl())
}

// Verify what was sent to the mock via domain queries:
mockVerificationClient.sakCount() shouldBe 1
mockVerificationClient.sedForRinaSak(rinaSaksnummer)
    .shouldContainInOrder("A012", "X008", "A004")
mockVerificationClient.medl().shouldHaveSize(1)
// Other queries: saker(), sakByFagsakNr(), oppgaver(), oppgaverByType(),
// journalposter(), bucInfo(), saksrelasjoner(), summary()
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

Most integration tests extend `MockServerTestBaseWithProsessManager`. The DB is truncated in `@BeforeEach` (via `OracleTestContainerBase.truncateAllTables()`), so each test starts from an empty database — there is no per-test cleanup to register.

```kotlin
class MyServiceIT : MockServerTestBaseWithProsessManager() {

    @Autowired
    private lateinit var service: MyService

    @Test
    fun `should integrate with database`() {
        // Given — DB is already empty (truncateAllTables ran in @BeforeEach)
        val fagsak = createTestFagsak()

        // When
        val result = service.process(fagsak.id)

        // Then
        assertThat(result).isNotNull()
    }
}
```

## Test Utilities

### Database isolation

Integration tests do not register cleanup. `OracleTestContainerBase.truncateAllTables()` runs in `@BeforeEach`: it reads all enabled foreign-key constraints from `user_constraints`, disables them, `TRUNCATE`s an explicit list of test-data tables (child tables before parents), then re-enables the constraints. Kodeverk and other static-data tables are deliberately excluded from the truncate list. The net effect: every test starts with a clean, empty set of business tables and can inspect leftover data afterwards for debugging.

### Test Data Builders

```kotlin
// E2ETestDataService in saksflyt module
val fagsak = e2eTestDataService.opprettFagsak(
    sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
    sakstype = Sakstyper.EU_EOS
)
```

## forTest DSL

Type-safe Kotlin DSL for creating test data with sensible defaults and nested entity support.

**Full reference**: See [references/fortest-dsl.md](references/fortest-dsl.md)

### Quick Examples

```kotlin
// Simple entity
val fagsak = Fagsak.forTest {
    tema = Sakstemaer.TRYGDEAVGIFT
    type = Sakstyper.FTRL
}

// Nested entities
val behandling = Behandling.forTest {
    fagsak {
        medBruker()
        medVirksomhet()
    }
    mottatteOpplysninger {
        type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        soeknad { landkoder("BE", "NL") }
    }
}

// Complex hierarchy
val behandlingsresultat = Behandlingsresultat.forTest {
    behandling { fagsak { type = Sakstyper.FTRL } }
    medlemskapsperiode {
        fom = LocalDate.of(2023, 1, 1)
        tom = LocalDate.of(2023, 12, 31)
        trygdeavgiftsperiode { trygdesats = 6.8.toBigDecimal() }
    }
}
```

### Available Factories

| Entity | Factory |
|--------|---------|
| `Fagsak` | `FagsakTestFactory` |
| `Behandling` | `BehandlingTestFactory` |
| `Behandlingsresultat` | `BehandlingsresultatTestFactory` |
| `Medlemskapsperiode` | `MedlemskapsperiodeTestFactory` |
| `Trygdeavgiftsperiode` | `TrygdeavgiftsperiodeTestFactory` |
| `Prosessinstans` | `ProsessinstansTestFactory` |

### KafkaOffsetChecker

`offsetIncreased(topic, groupId) { ... }` runs the block, then returns how much the consumer group's committed offset grew — useful to assert that a message was (or was NOT) consumed.

```kotlin
val delta = kafkaOffsetChecker.offsetIncreased(topic, groupId) {
    kafkaTemplate.send("teammelosys.eessi.v1-local", message)
    // wait for processing to complete...
}
delta shouldBe 0  // e.g. assert the message was NOT committed because it failed
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

**Full reference**: See [references/debugging.md](references/debugging.md)

Quick commands:
```bash
# Single test
scripts/run-tests.sh -pl service -Dtest=MyServiceTest#testMethodName

# Debug mode (attach debugger to port 5005)
mvn -pl service test -Dtest=MyTest -Dmaven.surefire.debug

# View full logs
cat /tmp/mvn.log
```

## Related Skills

- **kafka**: Kafka testing patterns
- **database**: Database schema for test data
- **saksflyt**: Saga testing patterns
