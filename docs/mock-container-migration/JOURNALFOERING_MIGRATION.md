# Journalføring Tests Migration to Container

## Status: Phase 5 Complete, Ready for Phase 6 Cleanup

**Last Updated:** 2025-12-16

## Goal

Migrate integration tests that use `JournalføringsoppgaveGenerator` to use the melosys-mock container instead of the in-process mock. This unifies the mock approach across:
- Local development
- E2E tests (melosys-e2e-tests)
- Integration tests (melosys-api)

## Migration Progress

### Phase 4: Migrate JournalfoeringBase Tests ✅ Complete

| Test | Tests | Status |
|------|-------|--------|
| `ContainerIkkeYrkesaktivVedtakIT` | 3 | ✅ Complete |
| `ContainerYrkesaktivEosVedtakIT` | 2 | ✅ Complete |
| `ContainerJournalfoeringIT` | 5 | ✅ Complete |
| `ContainerSedMottakBehandlingsTypeIT` | 2 | ✅ Complete |
| `ContainerSedMottakTestIT` | 11 | ✅ Complete |

### Phase 4b: Migrate AvgiftFakturering & Satsendring Tests ✅ Complete

| Test | Tests | Status |
|------|-------|--------|
| `ContainerSatsendringIT` | 4 | ✅ Complete |
| `ContainerSatsendringAdminControllerIT` | 1 | ✅ Complete |
| `ContainerPensjonistFtrlVedtakIT` | 2 | ✅ Complete |
| `ContainerYrkesaktivFtrlVedtakIT` | 2 | ✅ Complete |
| `ContainerÅrsavregningIT` | 5 | ✅ Complete |
| `ContainerEøsPensjonistIverksettIT` | 1 | ✅ Complete |

**New base classes created:**
- `ContainerSatsendringTestBase` (extends `ContainerJournalfoeringBase`)
- `ContainerAvgiftFaktureringTestBase` (extends `ContainerJournalfoeringBase`)

### Phase 5: Remaining ComponentTestBase Tests ✅ Complete

These tests use `ComponentTestBase` or `MockServerTestBaseWithProsessManager`:

| Test | Container Version | Status |
|------|-------------------|--------|
| `AktoerHistorikkServiceIT` | `ContainerAktoerHistorikkServiceIT` | ✅ Complete |
| `AvsluttBehandlingArt13JobbIT` | `ContainerAvsluttBehandlingArt13JobbIT` | ✅ Complete |
| `OpprettÅrsavregningIT` | `ContainerOpprettÅrsavregningIT` | ✅ Complete |
| `ÅrsavregningIkkeSkattepliktigeIT` | `ContainerÅrsavregningIkkeSkattepliktigeIT` | ✅ Complete |

### Tests Not Requiring Migration

These tests don't use `ComponentTestBase` - they use `OracleTestContainerBase`, `DataJpaTestBase`, or are pure Kafka tests:

| Test | Base Class | Notes |
|------|------------|-------|
| `AdminControllerApiKeyIT` | `OracleTestContainerBase` | API key testing, no external mock |
| `BehandlingsresultatServiceIT` | `DataJpaTestBase` | Pure JPA/repository test |
| `EessiMeldingConsumerIT` | None (embedded Kafka) | Kafka consumer test with mocked services |
| `EndreAktoerIdIT` | `OracleTestContainerBase` | Special test config |
| `FinnSakerForÅrsavregningIT` | `OracleTestContainerBase` | Query test, disabled |
| `KafkaSkipIT` | None (embedded Kafka) | Kafka error handling test |
| `LovvalgsperiodeServiceIT` | `DataJpaTestBase` | Pure JPA/repository test |
| `SaksflyThreadPoolTaskExecutorIT` | `OracleTestContainerBase` | Thread pool test |
| `SaksflytLåsreferanseIT` | `OracleTestContainerBase` | Saksflyt locking test |
| `SaksflytOppstartIT` | `OracleTestContainerBase` | Saksflyt startup test |
| `SedLåsMedSubProsesserIT` | `OracleTestContainerBase` | SED locking test |
| `SedLåsreferanseIT` | `OracleTestContainerBase` | SED locking test |
| `SoknadMottattConsumerIT` | None (embedded Kafka) | Kafka consumer test with mocked services |

### Phase 6: Cleanup (Ready to Start)

| Task | Status | Notes |
|------|--------|-------|
| Remove `JournalføringsoppgaveGenerator` | Pending | After original tests removed |
| Remove `JournalpostFactory` | Pending | |
| Remove original test files | Pending | Keep Container versions only |
| Consider removing `melosysmock/` package | Pending | |

## Inheritance Hierarchy (Container Tests)

```
ContainerMockServerTestBase
└── ContainerJournalfoeringBase
    ├── ContainerJournalfoeringIT
    ├── ContainerIkkeYrkesaktivVedtakIT
    ├── ContainerYrkesaktivEosVedtakIT
    ├── ContainerSedMottakTestIT
    ├── ContainerSedMottakBehandlingsTypeIT
    ├── ContainerSatsendringTestBase
    │   ├── ContainerSatsendringIT
    │   └── ContainerSatsendringAdminControllerIT
    └── ContainerAvgiftFaktureringTestBase
        ├── ContainerPensjonistFtrlVedtakIT
        ├── ContainerYrkesaktivFtrlVedtakIT
        ├── ContainerÅrsavregningIT
        └── ContainerEøsPensjonistIverksettIT
```

## Files Overview

### Container Test Files (New)
```
integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/
├── ContainerMockServerTestBase.kt
├── ContainerComponentTestBase.kt
├── ContainerJournalfoeringBase.kt
├── ContainerSatsendringTestBase.kt
├── ContainerAvgiftFaktureringTestBase.kt
├── ContainerJournalfoeringIT.kt
├── ContainerIkkeYrkesaktivVedtakIT.kt
├── ContainerYrkesaktivEosVedtakIT.kt
├── ContainerSedMottakTestIT.kt
├── ContainerSedMottakBehandlingsTypeIT.kt
├── ContainerSatsendringIT.kt
├── ContainerSatsendringAdminControllerIT.kt
├── ContainerPensjonistFtrlVedtakIT.kt
├── ContainerYrkesaktivFtrlVedtakIT.kt
├── ContainerÅrsavregningIT.kt
├── ContainerEøsPensjonistIverksettIT.kt
├── ContainerOpprettSakIT.kt
├── ContainerAktoerHistorikkServiceIT.kt
├── ContainerOpprettÅrsavregningIT.kt
├── ContainerÅrsavregningIkkeSkattepliktigeIT.kt
├── ContainerAvsluttBehandlingArt13JobbIT.kt
└── MockVerificationClient.kt
```

### Original Test Files (To Be Deprecated)
```
integrasjonstest/src/test/kotlin/no/nav/melosys/itest/
├── ComponentTestBase.kt
├── MockServerTestBaseWithProsessManager.kt
├── JournalfoeringBase.kt
├── JournalfoeringIT.kt
├── SedMottakTestIT.kt
├── SedMottakBehandlingsTypeIT.kt
├── AvgiftFaktureringTestBase.kt
├── SatsendringTestBase.kt
├── AktoerHistorikkServiceIT.kt
├── AvsluttBehandlingArt13JobbIT.kt
├── OpprettÅrsavregningIT.kt
├── ÅrsavregningIkkeSkattepliktigeIT.kt
└── vedtak/
    ├── IkkeYrkesaktivVedtakIT.kt
    ├── YrkesaktivEosVedtakIT.kt
    ├── PensjonistFtrlVedtakIT.kt
    ├── YrkesaktivFtrlVedtakIT.kt
    ├── ÅrsavregningIT.kt
    ├── EøsPensjonistIverksettIT.kt
    └── satsendring/
        ├── SatsendringIT.kt
        └── SatsendringAdminControllerIT.kt
```

## Benefits

1. **Single source of truth**: Same mock for local dev, e2e tests, and integration tests
2. **Realistic testing**: Actual HTTP calls like production
3. **Easier maintenance**: One mock codebase to maintain
4. **Consistency**: Same behavior across all test environments

## Summary Statistics

| Category | Count |
|----------|-------|
| Container tests created | 16 |
| Total test methods migrated | 55+ |
| Tests analyzed and not needing migration | 13 |
| Original tests ready for removal | All ComponentTestBase tests |
