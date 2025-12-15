# Mock Container Migration Progress

## Status: Option B Phase 3 Complete - Ready for Test Migration

**Last Updated:** 2025-12-15

## Overview

This document tracks the progress of migrating integration tests from in-process mock code to using verification endpoints via HTTP. We chose **Option B (Hybrid/Incremental)** approach.

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1: Add Verification Endpoints | **Complete** | Added to in-process mock |
| Phase 2: Create Testcontainer Setup | **Complete** | Container, DTOs, and Client created |
| Phase 3: Update Test Infrastructure | **Complete** | MockVerificationApi and ComponentTestBase updated |
| Phase 4: Migrate Tests | Not Started | Ready to begin |
| Phase 5: Remove Duplicate Code | Not Started | After container migration |

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

- [x] `GET /testdata/verification/medl` - List all MEDL perioder
- [x] `GET /testdata/verification/medl/count` - Count MEDL perioder
- [x] `GET /testdata/verification/sak` - List all saker
- [x] `GET /testdata/verification/sak/count` - Count saker
- [x] `GET /testdata/verification/sak/fagsak/{fagsakNr}` - Get sak by fagsakNr
- [x] `GET /testdata/verification/oppgave` - List all oppgaver
- [x] `GET /testdata/verification/oppgave/count` - Count oppgaver
- [x] `GET /testdata/verification/oppgave/type/{type}` - Filter oppgaver by type
- [x] `GET /testdata/verification/journalpost` - List all journalposter
- [x] `GET /testdata/verification/journalpost/count` - Count journalposter
- [x] `GET /testdata/verification/journalpost/{id}` - Get journalpost by ID
- [x] `GET /testdata/verification/journalpost/sak/{saksnummer}` - Get journalposter by sak
- [x] `GET /testdata/verification/melosys-eessi/sed/{rinaSaksnummer}` - List SED types for RINA sak
- [x] `GET /testdata/verification/melosys-eessi/sed` - Get all sedRepo data
- [x] `GET /testdata/verification/melosys-eessi/buc` - List BUC info
- [x] `GET /testdata/verification/melosys-eessi/bucer` - List BUC info (alias)
- [x] `GET /testdata/verification/melosys-eessi/saksrelasjoner` - List saksrelasjoner
- [x] `GET /testdata/verification/summary` - Get summary of all data counts
- [x] `DELETE /testdata/clear` - Clear all mock data

**Files Created/Updated:**
- `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/testdata/MockVerificationApi.kt` (NEW)
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/ComponentTestBase.kt` (UPDATED - added mockVerificationClient)
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/MelosysMockContainer.kt` (UPDATED - image name)
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/MockVerificationDtos.kt` (FIXED - unclosed comment)

### Phase 2: Testcontainer Setup

**Status:** Complete ✅ (for future container-based approach)

- [x] `MelosysMockContainer.kt` - Testcontainer wrapper (uses `melosys-docker-compose-mock:latest`)
- [x] `MockVerificationDtos.kt` - DTOs for verification responses
- [x] `MockVerificationClient.kt` - HTTP client for verification endpoints

### Phase 4: Migrate Tests (Next Steps)

Tests to migrate from direct repo access to MockVerificationClient:

| Test File | Current Pattern | New Pattern |
|-----------|-----------------|-------------|
| `YrkesaktivEosVedtakIT.kt` | `MedlRepo.repo.values.shouldHaveSize(1)` | `mockVerificationClient.medl().shouldHaveSize(1)` |
| `IkkeYrkesaktivVedtakIT.kt` | `MedlRepo.repo.values.shouldHaveSize(1)` | `mockVerificationClient.medl().shouldHaveSize(1)` |
| `SedMottakTestIT.kt` | `MelosysEessiRepo.sedRepo[rinaId]` | `mockVerificationClient.sedForRinaSak(rinaId)` |
| `SedMottakBehandlingsTypeIT.kt` | `OppgaveRepo.repo`, `JournalpostRepo.repo` | `mockVerificationClient.oppgaver()`, `.journalposter()` |
| `AvsluttBehandlingArt13JobbIT.kt` | `MedlRepo.repo.values.shouldHaveSize(1)` | `mockVerificationClient.medl().shouldHaveSize(1)` |

### Phase 5: Future - Full Container Migration

After Option B is stable, we can migrate to container-based mocks:
1. Use `MelosysMockContainer` with Testcontainers
2. Configure URLs to point to container instead of localhost:8093
3. Delete in-process mock code

## Files Overview

```
integrasjonstest/src/test/kotlin/no/nav/melosys/
├── itest/
│   ├── ComponentTestBase.kt           # Updated: exposes mockVerificationClient
│   └── mock/
│       ├── MelosysMockContainer.kt    # For future container-based approach
│       ├── MockVerificationClient.kt   # HTTP client for verification endpoints
│       └── MockVerificationDtos.kt     # DTOs for verification responses
└── melosysmock/
    └── testdata/
        └── MockVerificationApi.kt      # NEW: REST API for verification
```

## Decisions Made

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-12-15 | Use Option B (Hybrid) | Lower risk, incremental migration |
| 2025-12-15 | Add verification endpoints to in-process mock | Enables HTTP-based repo verification |
| 2025-12-15 | Use `melosys-docker-compose-mock:latest` | Built locally, not in GAR |
| 2025-12-15 | MockVerificationClient at localhost:8093 | Matches in-process mock server port |

## Next Steps

1. Migrate one test file (e.g., `YrkesaktivEosVedtakIT.kt`) as proof of concept
2. Migrate remaining test files in Phase 4
3. Consider full container migration (Phase 5) after stability is proven
