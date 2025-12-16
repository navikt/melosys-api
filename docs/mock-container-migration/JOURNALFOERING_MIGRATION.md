# Journalføring Tests Migration to Container

## Status: Phase 4b Complete, Phase 5 Pending

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

### Phase 5: Remaining Tests ❌ Not Yet Migrated

These tests use `ComponentTestBase` or other bases (not JournalfoeringBase):

| Test | Base Class | Complexity | Priority |
|------|------------|------------|----------|
| `AktoerHistorikkServiceIT` | `ComponentTestBase` | Low | Medium |
| `AvsluttBehandlingArt13JobbIT` | `ComponentTestBase` | Low | Low |
| `OpprettÅrsavregningIT` | `ComponentTestBase` | Medium | Medium |
| `ÅrsavregningIkkeSkattepliktigeIT` | `ComponentTestBase` | Medium | Medium |

These tests don't use JournalføringsoppgaveGenerator at all (may not need migration):

| Test | Notes |
|------|-------|
| `AdminControllerApiKeyIT` | API key testing, no mock needed |
| `BehandlingsresultatServiceIT` | Service unit test |
| `EessiMeldingConsumerIT` | Kafka consumer test |
| `EndreAktoerIdIT` | Special test config |
| `FinnSakerForÅrsavregningIT` | Query test |
| `KafkaSkipIT` | Kafka error handling |
| `LovvalgsperiodeServiceIT` | Service test |
| `SaksflyThreadPoolTaskExecutorIT` | Thread pool test |
| `SaksflytLåsreferanseIT` | Saksflyt test |
| `SaksflytOppstartIT` | Saksflyt startup test |
| `SedLåsMedSubProsesserIT` | SED locking test |
| `SedLåsreferanseIT` | SED locking test |
| `SoknadMottattConsumerIT` | Kafka consumer test |

### Phase 6: Cleanup (After All Migrated)

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
└── ContainerOpprettSakIT.kt
```

### Original Test Files (To Be Deprecated)
```
integrasjonstest/src/test/kotlin/no/nav/melosys/itest/
├── JournalfoeringBase.kt
├── JournalfoeringIT.kt
├── SedMottakTestIT.kt
├── SedMottakBehandlingsTypeIT.kt
├── AvgiftFaktureringTestBase.kt
├── SatsendringTestBase.kt
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
| Container tests created | 12 |
| Total test methods migrated | 38 |
| Remaining tests to analyze | 17 |
| Tests needing migration | ~4 |
| Tests not needing migration | ~13 |
