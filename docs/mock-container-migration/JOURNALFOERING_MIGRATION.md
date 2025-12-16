# Journalføring Tests Migration to Container

## Status: Phase 4 - In Progress (Testing)

**Last Updated:** 2025-12-16

## Goal

Migrate integration tests that use `JournalføringsoppgaveGenerator` to use the melosys-mock container instead of the in-process mock. This unifies the mock approach across:
- Local development
- E2E tests (melosys-e2e-tests)
- Integration tests (melosys-api)

## Current State

### In melosys-docker-compose mock
- `POST /testdata/jfr-oppgave` - creates journalføringsoppgave but returns `void`
- Uses `JournalPostService` with options: `medVedlegg`, `medLogiskVedlegg`

### In melosys-api in-process mock
- `JournalføringsoppgaveGenerator.opprettJfrOppgave()` - returns `Oppgave` object
- Uses `JournalpostFactory` (simpler, no vedlegg options)

### Problem
Tests need the created `Oppgave` back (for `id` and `journalpostId` fields)

## Implementation Plan

### Phase 1: Enhance Container Endpoint (melosys-docker-compose)

| Task | Status | Notes |
|------|--------|-------|
| Modify `/testdata/jfr-oppgave` to return `List<Oppgave>` | ✅ Complete | Changed in TestDataGenerator.kt |
| Build and push new container image | ✅ Complete | Built locally for testing |

**Changes needed in `TestDataGenerator.kt`:**
```kotlin
@PostMapping("/jfr-oppgave")
fun lagJournalføringsoppgave(@RequestBody request: OpprettJfrOppgaveRequest): List<Oppgave> {
    return (0 until request.antall).map { i ->
        val journalpostRequest = journalPostService.lagJournalPost(...)
        val journalpostMap = journalpostApi.opprettJournalpost(journalpostRequest, false)
        opprettJfrOppgave(journalpostRequest, journalpostMap["journalpostId"] as String, request.tilordnetRessurs)
    }
}
```

### Phase 2: Add Client Method (melosys-api)

| Task | Status | Notes |
|------|--------|-------|
| Add `opprettJfrOppgave()` to `MockVerificationClient` | ✅ Complete | Returns OppgaveVerificationDto |
| Add `OpprettJfrOppgaveRequest` DTO | ✅ Complete | Added to MockVerificationDtos.kt |

### Phase 3: Create Container Base Class

| Task | Status | Notes |
|------|--------|-------|
| Create `ContainerJournalfoeringBase` | ✅ Complete | Extends `ContainerMockServerTestBase` |

### Phase 4: Migrate Tests

| Test | Status | Notes |
|------|--------|-------|
| `IkkeYrkesaktivVedtakIT` | Pending | 3 tests |
| `YrkesaktivEosVedtakIT` | Pending | 2 tests |
| `JournalfoeringIT` | Pending | |
| `SedMottakBehandlingsTypeIT` | Pending | |
| Other tests extending `JournalfoeringBase` | Pending | |

### Phase 5: Cleanup (Optional)

| Task | Status | Notes |
|------|--------|-------|
| Remove `JournalføringsoppgaveGenerator` | Pending | After all tests migrated |
| Remove `JournalpostFactory` | Pending | |
| Consider removing entire `melosysmock/` package | Pending | |

## Files Overview

### melosys-docker-compose (to modify)
```
mock/src/main/kotlin/no/nav/melosys/melosysmock/testdata/
├── TestDataGenerator.kt       # Modify to return Oppgave
└── JournalPostService.kt      # Already has vedlegg support
```

### melosys-api (to create/modify)
```
integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/
├── MockVerificationClient.kt        # Add opprettJfrOppgave()
├── MockVerificationDtos.kt          # Add request DTO
├── ContainerJournalfoeringBase.kt   # NEW: Container version of JournalfoeringBase
└── ...
```

### melosys-api (tests to migrate)
```
integrasjonstest/src/test/kotlin/no/nav/melosys/itest/
├── JournalfoeringBase.kt                    # Current base class (in-process mock)
├── JournalfoeringIT.kt                      # → ContainerJournalfoeringBase
├── SedMottakBehandlingsTypeIT.kt            # → ContainerJournalfoeringBase
└── vedtak/
    ├── IkkeYrkesaktivVedtakIT.kt            # → ContainerJournalfoeringBase
    └── YrkesaktivEosVedtakIT.kt             # → ContainerJournalfoeringBase
```

## Benefits

1. **Single source of truth**: Same mock for local dev, e2e tests, and integration tests
2. **Realistic testing**: Actual HTTP calls like production
3. **Easier maintenance**: One mock codebase to maintain
4. **Consistency**: Same behavior across all test environments

## Decisions

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-12-16 | Return `List<Oppgave>` from endpoint | Supports `antall > 1` use case, caller uses `.first()` for single |
| 2025-12-16 | Keep vedlegg options in container | Already supported, more flexible than in-process mock |
