# Mock Container Migration Progress

## Status: Phase 5 Started - Container Infrastructure Ready

**Last Updated:** 2025-12-15

## Overview

This document tracks the progress of migrating integration tests from in-process mock code to using verification endpoints via HTTP. We chose **Option B (Hybrid/Incremental)** approach.

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1: Add Verification Endpoints | **Complete** | Added to in-process mock |
| Phase 2: Create Testcontainer Setup | **Complete** | Container, DTOs, and Client created |
| Phase 3: Update Test Infrastructure | **Complete** | MockVerificationApi and ComponentTestBase updated |
| Phase 4: Migrate Tests | **Complete** | All verification-based tests migrated |
| Phase 5: Container Migration | **In Progress** | Container infrastructure ready, tests pass |

## Architecture: Option B (Hybrid/Incremental)

We're using Option B which keeps the existing in-process mocks but adds HTTP verification endpoints:

```
┌─────────────────────────────────────────────────────────────┐
│                    Test JVM (port 8093)                      │
│  ┌──────────────┐  ┌─────────────────────────────────────┐  │
│  │  Test Code   │  │         melosys-api                 │  │
│  │              │  │  ┌─────────────────────────────┐    │  │
│  │ MockVerif-   │──┼──│  MockVerificationApi       │    │  │
│  │ icationClient│  │  │  (HTTP endpoints)          │    │  │
│  │              │  │  │           ↓                │    │  │
│  │              │  │  │  In-Process Mocks          │    │  │
│  │              │  │  │  (MedlRepo, SakRepo, etc.) │    │  │
│  └──────────────┘  └─────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Detailed Progress

### Phase 1 & 3 Combined: Verification Endpoints in In-Process Mock

**Status:** Complete ✅

Created `MockVerificationApi.kt` in `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/testdata/`:

- [x] `GET /testdata/verification/medl` - List all MEDL perioder (with sporingsinformasjon)
- [x] `GET /testdata/verification/medl/count` - Count MEDL perioder (returns `CountResponse`)
- [x] `GET /testdata/verification/sak` - List all saker
- [x] `GET /testdata/verification/sak/count` - Count saker (returns `CountResponse`)
- [x] `GET /testdata/verification/sak/fagsak/{fagsakNr}` - Get sak by fagsakNr
- [x] `GET /testdata/verification/oppgave` - List all oppgaver
- [x] `GET /testdata/verification/oppgave/count` - Count oppgaver (returns `CountResponse`)
- [x] `GET /testdata/verification/oppgave/type/{type}` - Filter oppgaver by type
- [x] `GET /testdata/verification/journalpost` - List all journalposter
- [x] `GET /testdata/verification/journalpost/count` - Count journalposter (returns `CountResponse`)
- [x] `GET /testdata/verification/journalpost/{id}` - Get journalpost by ID
- [x] `GET /testdata/verification/journalpost/sak/{saksnummer}` - Get journalposter by sak
- [x] `GET /testdata/verification/melosys-eessi/sed/{rinaSaksnummer}` - List SED types for RINA sak
- [x] `GET /testdata/verification/melosys-eessi/sed` - Get all sedRepo data
- [x] `GET /testdata/verification/melosys-eessi/buc` - List BUC info
- [x] `GET /testdata/verification/melosys-eessi/bucer` - List BUC info (alias)
- [x] `GET /testdata/verification/melosys-eessi/saksrelasjoner` - List saksrelasjoner
- [x] `GET /testdata/verification/summary` - Get summary of all data counts
- [x] `DELETE /testdata/clear` - Clear all mock data

### Phase 2: Testcontainer Setup

**Status:** Complete ✅

- [x] `MelosysMockContainer.kt` - Testcontainer wrapper (uses GAR image)
- [x] `MockVerificationDtos.kt` - DTOs for verification responses
- [x] `MockVerificationClient.kt` - HTTP client for verification endpoints

### Phase 4: Migrate Tests

**Status:** Complete ✅

Migrated tests from direct repo access to MockVerificationClient:

| Test File | Migration | Notes |
|-----------|-----------|-------|
| `YrkesaktivEosVedtakIT.kt` | ✅ Complete | 2 tests: `MedlRepo.repo.values` → `mockVerificationClient.medl()` |
| `IkkeYrkesaktivVedtakIT.kt` | ✅ Complete | 3 tests: `MedlRepo.repo.values` → `mockVerificationClient.medl()` |
| `SedMottakTestIT.kt` | ✅ Complete | `MelosysEessiRepo.sedRepo[rinaId]` → `mockVerificationClient.sedForRinaSak(rinaId)` |
| `SedMottakBehandlingsTypeIT.kt` | ✅ Complete | `oppgaveRepo.repo.values` → `mockVerificationClient.oppgaver()` |
| `AvsluttBehandlingArt13JobbIT.kt` | N/A | Uses `MedlRepo` for **setup** only, not verification |

### Phase 5: Container Migration

**Status:** In Progress 🔄

#### Completed

1. **Updated MockVerificationClient for container compatibility**
   - Count endpoints now return `CountResponse(count)` instead of raw `Int`
   - Compatible with both in-process mock and Docker container

2. **Updated in-process MockVerificationApi to match Docker mock**
   - Count endpoints return `CountResponse` for consistency
   - Added `CountResponse` data class

3. **Container Infrastructure Created**
   - `MelosysMockContainerTestBase.kt` - Base class with `@DynamicPropertySource` to configure URLs
   - `MelosysMockContainerIT.kt` - Standalone tests verifying container works
   - All container classes use GAR image: `europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest`

4. **Container Tests Pass** ✅
   - Container starts successfully (~30-40s startup time)
   - Health check passes
   - Verification endpoints accessible
   - Clear endpoint works

#### Remaining Work

1. **Full Integration Test with Container**
   - Create test that uses `MelosysMockContainerTestBase` with Spring Boot
   - Configure melosys-api to call container instead of in-process mock
   - This is the key step to prove full container migration works

2. **Optional: Remove In-Process Mock Code**
   - After proving container-based tests work
   - Delete `melosysmock/` package
   - Update ComponentTestBase to not clear static repos

## Files Overview

```
integrasjonstest/src/test/kotlin/no/nav/melosys/
├── itest/
│   ├── ComponentTestBase.kt              # exposes mockVerificationClient
│   ├── vedtak/
│   │   ├── YrkesaktivEosVedtakIT.kt      # uses mockVerificationClient.medl()
│   │   └── IkkeYrkesaktivVedtakIT.kt     # uses mockVerificationClient.medl()
│   ├── SedMottakTestIT.kt                # uses mockVerificationClient
│   ├── SedMottakBehandlingsTypeIT.kt     # uses mockVerificationClient
│   └── mock/
│       ├── MelosysMockContainer.kt       # Testcontainer wrapper
│       ├── MelosysMockContainerTestBase.kt # Base class with @DynamicPropertySource
│       ├── MelosysMockContainerIT.kt     # Standalone container tests ✅ PASSES
│       ├── MockVerificationClient.kt     # HTTP client (updated for CountResponse)
│       └── MockVerificationDtos.kt       # DTOs for verification responses
└── melosysmock/
    └── testdata/
        └── MockVerificationApi.kt        # REST API (updated for CountResponse)
```

## Decisions Made

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-12-15 | Use Option B (Hybrid) | Lower risk, incremental migration |
| 2025-12-15 | Add verification endpoints to in-process mock | Enables HTTP-based repo verification |
| 2025-12-15 | Use GAR image for testcontainers | `europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest` |
| 2025-12-15 | Count endpoints return `CountResponse` | Matches Docker mock API, ensures compatibility |
| 2025-12-15 | Handle null vs empty string in HTTP JSON | Use `shouldBeIn(null, "")` for optional fields |
| 2025-12-15 | Keep setup code with direct repo access | Only verification needs to use HTTP client |

## Next Steps

1. ✅ ~~Container infrastructure created and tested~~
2. **Create full integration test using MelosysMockContainerTestBase**
   - Test that melosys-api can call external services via container
   - Verify business logic works with container-based mocks
3. **Consider removing in-process mock code** (optional, after stability proven)
4. Remove unused direct repo imports from migrated tests (optional cleanup)
