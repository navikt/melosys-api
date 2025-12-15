# Mock Container Migration Progress

## Status: Phase 2 Complete

**Last Updated:** 2025-12-15

## Overview

This document tracks the progress of migrating integration tests from in-process mock code to using the `melosys-mock` Docker container as a Testcontainer.

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1: Add Verification Endpoints | In Progress | Delegated to melosys-docker-compose repo |
| Phase 2: Create Testcontainer Setup | **Complete** | Container, DTOs, and Client created |
| Phase 3: Update Test Infrastructure | Not Started | Blocked by Phase 1 |
| Phase 4: Migrate Tests | Not Started | Blocked by Phase 1 & 3 |
| Phase 5: Remove Duplicate Code | Not Started | |

## Detailed Progress

### Phase 1: Add Verification Endpoints to melosys-mock

**Repository:** `melosys-docker-compose/mock`
**Status:** Delegated to another agent

- [ ] Create `MockVerificationApi.kt` controller
  - [ ] `GET /testdata/verification/medl` - List all MEDL perioder
  - [ ] `GET /testdata/verification/medl/count` - Count MEDL perioder
  - [ ] `GET /testdata/verification/sak` - List all saker
  - [ ] `GET /testdata/verification/sak/count` - Count saker
  - [ ] `GET /testdata/verification/melosys-eessi/sed/{rinaSaksnummer}` - List SED types for RINA sak
  - [ ] `GET /testdata/verification/melosys-eessi/sed` - Get all sedRepo data
  - [ ] `GET /testdata/verification/melosys-eessi/buc` - List BUC info
  - [ ] `GET /testdata/verification/oppgave` - List all oppgaver
  - [ ] `GET /testdata/verification/oppgave/count` - Count oppgaver
  - [ ] `GET /testdata/verification/journalpost` - List all journalposter
  - [ ] `GET /testdata/verification/journalpost/count` - Count journalposter
- [ ] Update `DELETE /testdata/clear` to also clear:
  - [ ] `SakRepo.repo` and `SakRepo.fagsakNrSakRepo`
  - [ ] `MelosysEessiRepo.repo` and `MelosysEessiRepo.sedRepo`
  - [ ] `MelosysEessiApi.saksrelasjoner`
- [ ] Add Swagger documentation for new endpoints
- [ ] Test locally with docker-compose
- [ ] Create PR and merge to master
- [ ] Verify new Docker image is pushed to GAR

### Phase 2: Create Testcontainer Setup

**Repository:** `melosys-api-claude`
**Status:** Complete ✅

- [x] Add Testcontainers dependency to `integrasjonstest/pom.xml` (already present v2.0.2)
- [x] Create `MelosysMockContainer.kt`
  - [x] Configure Docker image from GAR
  - [x] Configure health check wait strategy
  - [x] Configure exposed ports
  - [x] Add logging with Slf4j
- [x] Create DTOs for verification responses
  - [x] `MedlemskapsunntakVerificationDto`
  - [x] `SakVerificationDto`
  - [x] `OppgaveVerificationDto`
  - [x] `JournalpostVerificationDto`
  - [x] Supporting DTOs (SporingsinformasjonDto, CountResponse, ClearResponse)
- [x] Create `MockVerificationClient.kt`
  - [x] Methods for each verification endpoint (medl, saker, oppgaver, journalposter)
  - [x] SED verification methods (sedForRinaSak, allSedRepo)
  - [x] Clear method
  - [x] Health check method
- [ ] Test container startup locally (blocked until Phase 1 verification endpoints are ready)

**Files Created:**
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/MelosysMockContainer.kt`
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/MockVerificationDtos.kt`
- `integrasjonstest/src/test/kotlin/no/nav/melosys/itest/mock/MockVerificationClient.kt`

### Phase 3: Update Test Infrastructure

- [ ] Create `application-test-container.properties` profile
  - [ ] Configure all integration URLs to use mock container
- [ ] Update `ComponentTestBase.kt`
  - [ ] Add `@Container` for mock container
  - [ ] Add `@DynamicPropertySource` for container URLs
  - [ ] Replace direct repo access in `@AfterEach` with client call
- [ ] Configure Docker network if needed
- [ ] Configure GAR authentication for CI

### Phase 4: Migrate Tests

| Test File | Status | Notes |
|-----------|--------|-------|
| `YrkesaktivEosVedtakIT.kt` | Not Started | Uses `MedlRepo.repo` |
| `IkkeYrkesaktivVedtakIT.kt` | Not Started | Uses `MedlRepo.repo` |
| `SedMottakTestIT.kt` | Not Started | Uses `MelosysEessiRepo.sedRepo` |
| `SedMottakBehandlingsTypeIT.kt` | Not Started | Uses `OppgaveRepo.repo`, `JournalpostRepo.repo` |
| `AvsluttBehandlingArt13JobbIT.kt` | Not Started | Uses `MedlRepo.repo` |
| `ComponentTestBase.kt` | Not Started | Uses multiple repos |

### Phase 5: Remove Duplicate Code

- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/aareg/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/azuread/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/config/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/dokprod/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/inngang/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/inntekt/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/journalpost/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/kodeverk/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/medl/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/melosyseessi/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/oppgave/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/organisasjon/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/pdl/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/person/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/reststs/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/sak/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/testdata/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/tilgangsmaskinen/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/utbetal/`
- [ ] Delete `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/utils/`
- [ ] Update any remaining imports/references
- [ ] Final test run to verify all tests pass

## Blockers

| Blocker | Status | Resolution |
|---------|--------|------------|
| Phase 1 must complete first | Active | Waiting for verification endpoints in melosys-mock |

## Decisions Made

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-12-15 | Use existing melosys-mock Docker image | Eliminates duplication, ensures consistency |
| 2025-12-15 | Add verification endpoints to mock | Enables HTTP-based repo verification |
| 2025-12-15 | Use static container with `@Container` | Faster tests by reusing container |
| 2025-12-15 | Delegate Phase 1 to melosys-docker-compose repo | Separation of concerns |

## Open Questions

1. Should we version-pin the Docker image for stability?
2. Do we need a shared Docker network for container-to-API communication?
3. Should we implement a fallback to in-process mocks for local debugging?

## Related Documents

- [Migration Plan](./MIGRATION-PLAN.md) - Detailed implementation plan
