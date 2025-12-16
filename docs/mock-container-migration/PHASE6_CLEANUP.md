# Phase 6: Cleanup - Remove Original In-Process Mock Tests

## Status: Ready to Start

**Created:** 2025-12-16

## Goal

Remove the original test files that use the in-process mock now that all tests have been migrated to use the container-based mock via `ContainerComponentTestBase`.

## Prerequisites

All ComponentTestBase tests have been migrated:
- ContainerAktoerHistorikkServiceIT ✅
- ContainerAvsluttBehandlingArt13JobbIT ✅
- ContainerOpprettÅrsavregningIT ✅
- ContainerÅrsavregningIkkeSkattepliktigeIT ✅
- ContainerSedMottakTestIT ✅
- ContainerJournalfoeringIT ✅
- ContainerIkkeYrkesaktivVedtakIT ✅
- ContainerYrkesaktivEosVedtakIT ✅
- ContainerSedMottakBehandlingsTypeIT ✅
- ContainerSatsendringIT ✅
- ContainerSatsendringAdminControllerIT ✅
- ContainerPensjonistFtrlVedtakIT ✅
- ContainerYrkesaktivFtrlVedtakIT ✅
- ContainerÅrsavregningIT ✅
- ContainerEøsPensjonistIverksettIT ✅
- ContainerOpprettSakIT ✅

## Cleanup Tasks

### Task 1: Remove Original Test Files (Base Classes)
| File | Status | Notes |
|------|--------|-------|
| `ComponentTestBase.kt` | Pending | Main base class |
| `MockServerTestBaseWithProsessManager.kt` | Pending | Extends ComponentTestBase |
| `JournalfoeringBase.kt` | Pending | Extends MockServerTestBaseWithProsessManager |
| `AvgiftFaktureringTestBase.kt` | Pending | Extends JournalfoeringBase |
| `SatsendringTestBase.kt` | Pending | Extends JournalfoeringBase |

### Task 2: Remove Original Test Files (Test Classes)
| File | Status | Notes |
|------|--------|-------|
| `AktoerHistorikkServiceIT.kt` | Pending | |
| `AvsluttBehandlingArt13JobbIT.kt` | Pending | |
| `OpprettÅrsavregningIT.kt` | Pending | |
| `ÅrsavregningIkkeSkattepliktigeIT.kt` | Pending | |
| `SedMottakTestIT.kt` | Pending | |
| `SedMottakBehandlingsTypeIT.kt` | Pending | |
| `JournalfoeringIT.kt` | Pending | |
| `vedtak/IkkeYrkesaktivVedtakIT.kt` | Pending | |
| `vedtak/YrkesaktivEosVedtakIT.kt` | Pending | |
| `vedtak/PensjonistFtrlVedtakIT.kt` | Pending | |
| `vedtak/YrkesaktivFtrlVedtakIT.kt` | Pending | |
| `vedtak/ÅrsavregningIT.kt` | Pending | |
| `vedtak/EøsPensjonistIverksettIT.kt` | Pending | |
| `vedtak/satsendring/SatsendringIT.kt` | Pending | |
| `vedtak/satsendring/SatsendringAdminControllerIT.kt` | Pending | |

### Task 3: Remove In-Process Mock Code
| File/Package | Status | Notes |
|--------------|--------|-------|
| `melosysmock/` package | Pending | Evaluate what can be removed |
| `JournalføringsoppgaveGenerator.kt` | Pending | Used only by old tests |
| `JournalpostFactory.kt` | Pending | Used only by old tests |
| In-process mock repos (MedlRepo, OppgaveRepo, etc.) | Pending | Check for other usages |

### Task 4: Verify and Clean Up
| Task | Status | Notes |
|------|--------|-------|
| Run all Container tests | Pending | Ensure nothing broke |
| Check for unused imports/dependencies | Pending | |
| Update any documentation | Pending | |

## Approach

1. **Start with test files** - Remove original test classes first (Task 2)
2. **Then base classes** - Remove base classes bottom-up (Task 1)
3. **Then mock code** - Carefully evaluate and remove in-process mock code (Task 3)
4. **Verify** - Run all tests and check for issues (Task 4)

## Commands

```bash
# Run all Container tests to verify they work
~/.claude/scripts/run-tests.sh -pl integrasjonstest --integration -Dtest="Container*IT"

# Run full integration test suite
~/.claude/scripts/run-tests.sh -pl integrasjonstest -am --integration
```

## Notes

- The `melosysmock/` package contains both in-process mock code AND the `MockVerificationClient` + DTOs used by Container tests
- Only remove in-process mock code, keep verification endpoints and DTOs
- Some in-process mock code may still be used by tests not in scope (OracleTestContainerBase tests)
