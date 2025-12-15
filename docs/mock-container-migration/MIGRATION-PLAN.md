# Migration Plan: melosys-mock as Testcontainer

## Executive Summary

Replace the in-process mock code in `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock` with the Docker image `melosys-mock` from GAR, running as a Testcontainer. This eliminates code duplication and ensures integration tests use the same mock as local development and E2E tests.

## Current State

### Duplicate Mock Code
Two nearly identical mock implementations exist:

| Location | Purpose |
|----------|---------|
| `melosys-docker-compose/mock/` | Used for local development and E2E tests |
| `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/` | Used for integration tests |

### Current Test Verification Pattern
Tests verify mock state by direct repo access:
```kotlin
// Current approach - direct repo access
MedlRepo.repo.values.shouldHaveSize(1)
MelosysEessiRepo.sedRepo.get(rinaSaksnummer)!!.shouldContainInOrder(SedType.A012, SedType.X008)
SakRepo.clear()
```

### Docker Image Location
```
europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-mock:latest
```
- Multi-arch: `linux/amd64`, `linux/arm64`
- Spring Boot 2.7.18

## Target State

### Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                    Integration Test JVM                          │
│  ┌─────────────────┐    ┌──────────────────┐                    │
│  │  ComponentTest  │───▶│ MockVerification │                    │
│  │                 │    │     Client       │                    │
│  └─────────────────┘    └────────┬─────────┘                    │
│          │                       │                               │
│          │                       │ HTTP                          │
│          ▼                       ▼                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    melosys-api (SUT)                        ││
│  └───────────────────────────┬─────────────────────────────────┘│
│                              │ HTTP                              │
└──────────────────────────────┼──────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                  Docker (Testcontainer)                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    melosys-mock                             ││
│  │  ┌────────────────────────────────────────────────────────┐ ││
│  │  │ Verification API (/testdata/verification/*)            │ ││
│  │  │ - GET  /medl                                           │ ││
│  │  │ - GET  /sak                                            │ ││
│  │  │ - GET  /melosys-eessi/sed/{rinaSaksnummer}            │ ││
│  │  │ - GET  /oppgave                                        │ ││
│  │  │ - DELETE /clear (existing)                             │ ││
│  │  └────────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### New Test Verification Pattern
```kotlin
// New approach - HTTP client to verification endpoints
mockVerificationClient.medl().shouldHaveSize(1)
mockVerificationClient.sedForRinaSak(rinaSaksnummer).shouldContainInOrder(SedType.A012, SedType.X008)
mockVerificationClient.clear()
```

## Implementation Phases

### Phase 1: Add Verification Endpoints to melosys-mock
**Repository:** `melosys-docker-compose/mock`

Create new REST controller for test verification:

```kotlin
@RestController
@RequestMapping("/testdata/verification")
class MockVerificationApi(
    private val journalpostRepo: JournalpostRepo,
    private val oppgaveRepo: OppgaveRepo,
) {
    // GET /testdata/verification/medl
    @GetMapping("/medl")
    fun getMedlPerioder(): List<MedlemskapsunntakForGet> = MedlRepo.repo.values.toList()

    @GetMapping("/medl/count")
    fun getMedlCount(): Int = MedlRepo.repo.size

    // GET /testdata/verification/sak
    @GetMapping("/sak")
    fun getSaker(): List<Sak> = SakRepo.repo.values.toList()

    @GetMapping("/sak/count")
    fun getSakCount(): Int = SakRepo.repo.size

    // GET /testdata/verification/melosys-eessi/sed/{rinaSaksnummer}
    @GetMapping("/melosys-eessi/sed/{rinaSaksnummer}")
    fun getSedTyper(@PathVariable rinaSaksnummer: String): List<String> =
        MelosysEessiRepo.sedRepo[rinaSaksnummer]?.map { it.name } ?: emptyList()

    @GetMapping("/melosys-eessi/sed")
    fun getAllSedRepo(): Map<String, List<String>> =
        MelosysEessiRepo.sedRepo.mapValues { it.value.map { s -> s.name } }

    // GET /testdata/verification/oppgave
    @GetMapping("/oppgave")
    fun getOppgaver(): List<Oppgave> = oppgaveRepo.repo.values.toList()

    @GetMapping("/oppgave/count")
    fun getOppgaveCount(): Int = oppgaveRepo.repo.size

    // GET /testdata/verification/journalpost
    @GetMapping("/journalpost")
    fun getJournalposter(): List<JournalpostModell> = journalpostRepo.repo.values.toList()

    @GetMapping("/journalpost/count")
    fun getJournalpostCount(): Int = journalpostRepo.repo.size
}
```

Update existing `/testdata/clear` to also clear:
- `SakRepo.repo.clear()` and `SakRepo.fagsakNrSakRepo.clear()`
- `MelosysEessiRepo.repo.clear()` and `MelosysEessiRepo.sedRepo.clear()`
- `MelosysEessiApi.saksrelasjoner.clear()`

### Phase 2: Create Testcontainer Setup in integrasjonstest
**Repository:** `melosys-api-claude`

#### 2.1 Add Dependencies
```xml
<!-- integrasjonstest/pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

#### 2.2 Create MelosysMockContainer
```kotlin
// integrasjonstest/src/test/kotlin/no/nav/melosys/itest/container/MelosysMockContainer.kt
class MelosysMockContainer : GenericContainer<MelosysMockContainer>(
    DockerImageName.parse("europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-mock:latest")
) {
    init {
        withExposedPorts(8080)
        waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200))
    }

    fun getBaseUrl(): String = "http://${host}:${getMappedPort(8080)}"
}
```

#### 2.3 Create MockVerificationClient
```kotlin
// integrasjonstest/src/test/kotlin/no/nav/melosys/itest/client/MockVerificationClient.kt
class MockVerificationClient(
    private val baseUrl: String,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    fun medl(): List<MedlemskapsunntakDto> =
        restTemplate.getForObject("$baseUrl/testdata/verification/medl", Array<MedlemskapsunntakDto>::class.java)?.toList() ?: emptyList()

    fun medlCount(): Int =
        restTemplate.getForObject("$baseUrl/testdata/verification/medl/count", Int::class.java) ?: 0

    fun saker(): List<SakDto> =
        restTemplate.getForObject("$baseUrl/testdata/verification/sak", Array<SakDto>::class.java)?.toList() ?: emptyList()

    fun sedForRinaSak(rinaSaksnummer: String): List<String> =
        restTemplate.getForObject("$baseUrl/testdata/verification/melosys-eessi/sed/$rinaSaksnummer", Array<String>::class.java)?.toList() ?: emptyList()

    fun oppgaver(): List<OppgaveDto> =
        restTemplate.getForObject("$baseUrl/testdata/verification/oppgave", Array<OppgaveDto>::class.java)?.toList() ?: emptyList()

    fun clear() {
        restTemplate.delete("$baseUrl/testdata/clear")
    }
}
```

### Phase 3: Update Test Infrastructure

#### 3.1 Modify ComponentTestBase
```kotlin
@ActiveProfiles("test-container") // New profile
class ComponentTestBase : OracleTestContainerBase() {
    companion object {
        @Container
        @JvmStatic
        val mockContainer = MelosysMockContainer()

        @DynamicPropertySource
        @JvmStatic
        fun mockProperties(registry: DynamicPropertyRegistry) {
            registry.add("melosys.integrasjoner.base-url") { mockContainer.getBaseUrl() }
            // Add other integration URLs...
        }
    }

    protected val mockVerificationClient by lazy {
        MockVerificationClient(mockContainer.getBaseUrl())
    }

    @AfterEach
    fun clearMockData() {
        mockVerificationClient.clear()
        fakeUnleash.enableAll()
    }
}
```

#### 3.2 Create application-test-container.properties
```properties
# Point all integrations to the mock container
melosys.integrasjoner.medl.url=${MOCK_URL}/api/v1
melosys.integrasjoner.sak.url=${MOCK_URL}
melosys.integrasjoner.oppgave.url=${MOCK_URL}
# ... etc
```

### Phase 4: Migrate Tests

#### 4.1 Example Migration
**Before:**
```kotlin
@Test
fun `test vedtak creates medl periode`() {
    // ... test setup and execution ...

    MedlRepo.repo.values
        .shouldHaveSize(1)
        .first()
        .apply {
            fraOgMed shouldBe LocalDate.of(2023, 1, 1)
            tilOgMed shouldBe LocalDate.of(2023, 2, 1)
            status shouldBe "GYLD"
        }
}
```

**After:**
```kotlin
@Test
fun `test vedtak creates medl periode`() {
    // ... test setup and execution ...

    mockVerificationClient.medl()
        .shouldHaveSize(1)
        .first()
        .apply {
            fraOgMed shouldBe LocalDate.of(2023, 1, 1)
            tilOgMed shouldBe LocalDate.of(2023, 2, 1)
            status shouldBe "GYLD"
        }
}
```

### Phase 5: Remove Duplicate Code

After all tests are migrated, delete:
```
integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/
├── aareg/
├── azuread/
├── config/
├── dokprod/
├── inngang/
├── inntekt/
├── journalpost/
├── kodeverk/
├── medl/
├── melosyseessi/
├── oppgave/
├── organisasjon/
├── pdl/
├── person/
├── reststs/
├── sak/
├── testdata/
├── tilgangsmaskinen/
├── utbetal/
└── utils/
```

## Repos and Their Usage

| Repo | Current Usage | Verification Endpoint |
|------|---------------|----------------------|
| `MedlRepo.repo` | Verify MEDL perioder created | `GET /testdata/verification/medl` |
| `SakRepo.repo` | Verify arkivsak created | `GET /testdata/verification/sak` |
| `MelosysEessiRepo.sedRepo` | Verify SEDs sent | `GET /testdata/verification/melosys-eessi/sed/{rinaSaksnummer}` |
| `MelosysEessiRepo.repo` | Verify BUC info | `GET /testdata/verification/melosys-eessi/buc` |
| `OppgaveRepo.repo` | Verify oppgaver created | `GET /testdata/verification/oppgave` |
| `JournalpostRepo.repo` | Verify journalposter | `GET /testdata/verification/journalpost` |
| All repos | Clear between tests | `DELETE /testdata/clear` |

## Files That Need Migration

Based on grep analysis, these test files use direct repo access:

| File | Repos Used |
|------|------------|
| `YrkesaktivEosVedtakIT.kt` | `MedlRepo.repo` |
| `IkkeYrkesaktivVedtakIT.kt` | `MedlRepo.repo` |
| `SedMottakTestIT.kt` | `MelosysEessiRepo.sedRepo` |
| `SedMottakBehandlingsTypeIT.kt` | `OppgaveRepo.repo`, `JournalpostRepo.repo` |
| `AvsluttBehandlingArt13JobbIT.kt` | `MedlRepo.repo` |
| `ComponentTestBase.kt` | `SakRepo`, `MedlRepo.repo`, `MelosysEessiRepo.sedRepo` |

## Dependencies and Requirements

### GAR Authentication
Tests need authentication to pull from GAR:
```kotlin
// Option 1: Use GITHUB_TOKEN (CI)
// Option 2: Use gcloud auth for local development
```

### Network Configuration
The mock container needs to be accessible from:
1. The test JVM (for verification calls)
2. The melosys-api under test (for integration calls)

This may require a shared Docker network in the test setup.

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| GAR authentication in CI | Use existing GitHub Actions secrets |
| Container startup time | Use shared container across tests with `@Container` static |
| Network connectivity | Configure shared Docker network |
| Breaking changes in mock | Version pin the Docker image |
| Test isolation | Clear repos after each test |

## Success Criteria

1. All integration tests pass using the container-based mock
2. No duplicate mock code in `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/`
3. Same mock behavior as local development and E2E tests
4. CI pipeline runs successfully with GAR authentication

## Timeline Estimate

| Phase | Effort |
|-------|--------|
| Phase 1: Verification Endpoints | 1-2 days |
| Phase 2: Testcontainer Setup | 1 day |
| Phase 3: Test Infrastructure | 1 day |
| Phase 4: Migrate Tests | 2-3 days |
| Phase 5: Cleanup | 0.5 days |
| **Total** | **~1 week** |
