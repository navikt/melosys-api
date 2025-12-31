# Test DSL Conversion Progress

## Overview

Converting tests to use the immutable `forTest` DSL pattern and removing mutable test data patterns.

## Orchestration Prompt

Use this prompt to run the conversion process:

```
You are orchestrating the test DSL conversion for the melosys-api project.

## Reference Documentation

First, read the testing skill for context:
- `.claude/skills/testing/skill.md` - Testing patterns overview
- `.claude/skills/testing/references/fortest-dsl.md` - Complete forTest DSL reference

## Process

1. **Check Progress**: Read `.claude/progress/test-dsl-conversion.md` for current status

2. **Select Next File**: Choose the next unconverted file from priority list, or use:
   ```bash
   # Find files with mutable patterns not yet using forTest
   grep -l "Behandling()\.apply\|Fagsak()\.apply" --include="*Test*.kt" -r service/src/test | head -5
   ```

3. **Launch Converter**: Use the kotlin-test-refactorer agent:
   ```
   Task(subagent_type="kotlin-test-refactorer", prompt="Convert {FILE_PATH} to use the forTest DSL")
   ```

4. **Verify**: After agent completes, confirm tests pass:
   ```bash
   ~/.claude/scripts/run-tests.sh -pl {module} -Dtest={TestClassName}
   ```

5. **Update Progress**: Mark file as completed in progress document

6. **Handle Failures**: If tests fail:
   - Revert: `git checkout -- {file}`
   - Log issue in progress document
   - Move to next file

7. **Continue**: Repeat until batch is complete

## Priority Order

1. service/avgift/** (trygdeavgift calculations)
2. service/behandling/** (core business logic)
3. saksflyt/steg/** (saga steps)
4. frontend-api/** (API controllers)

## Success Criteria

- All tests pass after conversion
- No remaining `Entity().apply` patterns
- No `lateinit var` for domain entities
- Code review shows cleaner, more readable tests
```

## Statistics

| Metric | Count |
|--------|-------|
| Total test files (Kotlin) | 480 |
| Integration test files | 31 |
| Files already using `forTest` DSL | 213 (44%) |
| Files with mutable `.apply{}` patterns | 83 |
| Files with `lateinit var` test data | 138 |
| Estimated files needing conversion | ~150-200 |

### Conversion Summary
| Phase | Files Converted | Tests Passing |
|-------|-----------------|---------------|
| Phase 1: service/avgift | 8 | 88 |
| Phase 1: service/behandling | 7 (+1 no changes) | 116 |
| Phase 1: service/dokument | 13 | 213 |
| Phase 1: service/eessi | 7 | 50 |
| Phase 1: service/sak | 4 (+1 no changes) | 38 |
| Phase 1: service/vedtak | 2 | 19 |
| **Total** | **41** | **524** |

### New Test Factories Created
- `VilkaarsresultatTestFactory` - Created during Phase 1 service/behandling conversion
- Extended `BehandlingsresultatTestFactory` with `vilkaarsresultat {}` DSL builder
- Extended `SaksopplysningTestFactory` with `kilder` property
- Extended `SaksopplysningTestFactory.SedDokumentBuilder` with `sedType` property (Phase 1 service/eessi)
- Extended `AnmodningsperiodeTestFactory.Builder` with `medlPeriodeID` property (Phase 1 service/eessi)

### By Module

| Module | Test Files | Priority |
|--------|------------|----------|
| service | 226 | High |
| saksflyt | 71 | High |
| frontend-api | 52 | Medium |
| domain | 49 | Low (already has factories) |
| integrasjon | 49 | Medium |

## Anti-Patterns to Remove

### 1. Mutable Object Creation
```kotlin
// BAD: Creating then mutating
val behandling = Behandling().apply {
    id = 1L
    status = Behandlingsstatus.OPPRETTET
}
behandling.fagsak = fagsak  // Mutating after creation

// GOOD: Immutable DSL
val behandling = Behandling.forTest {
    id = 1L
    status = Behandlingsstatus.OPPRETTET
    fagsak { /* nested config */ }
}
```

### 2. Lateinit Shared State
```kotlin
// BAD: Shared mutable state in @BeforeEach
private lateinit var defaultBehandling: Behandling

@BeforeEach
fun setup() {
    defaultBehandling = Behandling()
    defaultBehandling.status = Behandlingsstatus.OPPRETTET
}

@Test
fun test1() {
    defaultBehandling.status = Behandlingsstatus.AVSLUTTET  // Mutation!
}

// GOOD: Create fresh in each test
@Test
fun test1() {
    val behandling = Behandling.forTest {
        status = Behandlingsstatus.AVSLUTTET
    }
}
```

### 3. Test-Specific Factories Not Using DSL
```kotlin
// BAD: Custom factories duplicating DSL functionality
object DokgenTestData {
    fun lagBehandling() = Behandling().apply { ... }
}

// GOOD: Use or extend the DSL
val behandling = Behandling.forTest { /* config */ }
```

## Conversion Progress

### Phase 1: High-Impact Service Tests
- [x] service/avgift/** - 8 files converted (88 tests passing)
  - EøsPensjonistTrygdeavgiftsberegningServiceTest.kt (14 tests)
  - ManglendeFakturabetalingConsumerTest.kt (4 tests)
  - EøsPensjonistTrygdeavgiftsberegningValidatorTest.kt (7 tests)
  - SatsendringFinnerTest.kt (11 tests)
  - FaktureringEventListenerTest.kt (4 tests)
  - ÅrsavregningServiceOpprettTest.kt (3 tests)
  - SkattehendelserConsumerTest.kt (4 tests)
  - TrygdeavgiftsberegningValidatorTest.kt (41 tests)
- [x] service/behandling/** - 7 files converted (116 tests passing)
  - BehandlingsresultatServiceTest.kt (16 tests)
  - BehandlingServiceTest.kt (55 tests)
  - BehandlingEventListenerTest.kt (5 tests)
  - AngiBehandlingsresultatServiceTest.kt (10 tests)
  - AvsluttArt13BehandlingServiceTest.kt (11 tests)
  - ReplikerBehandlingsresultatServiceTest.kt (8 tests)
  - VilkaarsresultatServiceTest.kt (11 tests)
  - AvsluttArt13BehandlingJobbTest.kt (no changes needed - only mocks)
- [x] service/dokument/** - 13 files converted (213 tests passing)
  - BrevmottakerServiceTest.kt (31 tests)
  - DokumentServiceTest.kt (6 tests)
  - BrevDataServiceTest.kt (17 tests)
  - BrevDataByggerA001Test.kt (12 tests)
  - BrevDataByggerInnvilgelseTest.kt (12 tests)
  - BrevDataByggerInnvilgelseFlereLandTest.kt (4 tests)
  - A1MapperTest.kt (25 tests)
  - AttestMapperTest.kt (1 test)
  - AvslagYrkesaktivMapperTest.kt (3 tests)
  - DokgenMapperDatahenterTest.kt (9 tests)
  - ÅrsavregningVedtakMapperTest.kt (12 tests)
  - EessiServiceTest.kt (42 tests)
  - SedDataByggerTest.kt (39 tests)
- [x] service/eessi/** - 7 files converted (50 tests passing)
  - [x] AdminInnvalideringSedRuterTest.kt (8 tests)
  - [x] AnmodningOmUnntakSedRuterTest.kt (4 tests)
  - [x] SvarAnmodningUnntakSedRuterTest.kt (5 tests)
  - [x] UnntaksperiodeSedRuterTest.kt (6 tests)
  - [x] DefaultSedRuterTest.kt (8 tests)
  - [x] AdminFjernmottakerSedRuterTest.kt (8 tests)
  - [x] ArbeidFlereLandSedRuterTest.kt (11 tests)
- [x] service/sak/** - 4 files converted (+1 no changes needed) (38 tests passing)
  - [x] AnnullerSakServiceTest.kt (3 tests)
  - [x] EndreSakServiceTest.kt (11 tests)
  - [x] TrygdeavgiftServiceTest.kt (9 tests)
  - [x] FagsakServiceTest.kt (15 tests) - minimal pattern kept for complex relationship
- [x] service/vedtak/** - 2 files converted (19 tests passing)
  - [x] EosVedtakServiceKtTest.kt (7 tests)
  - [x] FtrlVedtakServiceTest.kt (12 tests)

### Phase 1.5: Remaining Service Tests (2025-12-31)
Files converted from legacy patterns (SaksbehandlingDataFactory, .apply after factory):
- [x] JournalfoeringServiceTest.kt (49 tests, ~20+ patterns)
- [x] OppgaveplukkerTest.kt (10 tests, 9 patterns)
- [x] LandvelgerServiceTest.kt (23 tests, 11 patterns)
- [x] KontrollTest.kt (18 tests, ~30 patterns)
- [x] AnmodningUnntakKontrollServiceTest.kt (9 tests, 12 patterns)
- [x] MedlPeriodeServiceTest.kt (18 tests, 20 patterns)
- [x] OppgaveServiceTest.kt (23 tests, 16 patterns)
- [x] MedlAnmodningsperiodeServiceTest.kt (2 tests) - already using forTest DSL
- [x] RegisteropplysningerServiceTest.kt (7 tests) - already using forTest DSL
- [x] SaksopplysningEventListenerTest.kt (4 tests) - already using forTest DSL
- [x] LovvalgsperiodeServiceTest.kt (17 tests, 16 patterns)
- [x] SaksbehandlingReglerTest.kt (43 tests, 1 pattern) - mostly compliant
- [x] OverlappendeMedlemskapsperioderReglerTest.kt (30 tests, ~15 patterns)
- [x] OpprettForslagMedlemskapsperiodeServiceTest.kt (12 tests, ~20 patterns)
- [x] MedlemskapsperiodeServiceTest.kt (tests pass, ~40 patterns)
- [x] AvklarteMedfolgendeFamilieServiceTest.kt (13 tests, 8 patterns)

**Phase 1.5 Summary (2025-12-31):**
- 16 files processed
- 12 files converted (~200+ patterns total)
- 4 files already compliant
- ~300 tests verified passing

### Phase 2: Saksflyt Tests ✅ COMPLETED
- [x] saksflyt/steg/behandling/** - 11 files (100% using forTest)
- [x] saksflyt/steg/sed/** - 14 files (100% using forTest)
- [x] saksflyt/steg/brev/** - 10 files (100% using forTest)
- [x] saksflyt/steg/medl/** - 5 files (100% using forTest)

Commits: `4c38ace`, `ae33185`, `74b05bc`, `740c541`

### Phase 3: Frontend-API Tests (2025-12-31)
Files converted from legacy patterns:
- [x] BehandlingControllerTest.kt (4 tests, 2 patterns)
- [x] VedtakControllerTest.kt (5 tests) - only DTOs, no changes needed
- [x] LovvalgsperiodeControllerTest.kt (6 tests, 2 patterns)
- [x] MottatteOpplysningerControllerTest.kt (3 tests, 2 patterns)
- [x] AktoerControllerTest.kt (4 tests, 2 patterns + enhanced FagsakTestFactory)
- [x] AvklartefaktaControllerTest.kt (3 tests, 2 patterns + AvklartefaktaRegistreringTestFactory)
- [x] AnmodningUnntakControllerTest.kt (2 tests) - only DTOs, no changes needed
- [x] AvvisUtpekingControllerTest.kt (1 test) - only DTOs, no changes needed
- [x] HelseutgiftDekkesPeriodeControllerTest.kt (5 tests, 1 pattern)
- [x] SaksopplysningerTilDtoTest.kt (2 tests, 4 patterns + arbeidsforholdForTest DSL)
- [x] BrevmalListeByggerTest.kt (39 tests, 10 patterns)
- [x] TrygdeavtaleControllerTest.kt (7 tests, 6 patterns)
- [x] FagsakControllerTest.kt (32 tests, 9 patterns)
- [x] MedlemskapsperiodeControllerTest.kt (2 tests, 1 pattern)
- [x] BehandlingsnotatControllerTest.kt (3 tests, 2 patterns + BehandlingsnotatTestFactory)

### Phase 4: Remaining
- [ ] integrasjon/** - as needed
- [ ] statistikk/** - as needed

## Validation Checklist

For each converted file:
- [ ] Tests pass
- [ ] No `lateinit var` for domain entities
- [ ] No `.apply{}` after construction
- [ ] Uses `forTest` DSL for all domain entities
- [ ] Nested entities configured in DSL blocks
- [ ] No mutation of test data between tests

## How to Run Validation

```bash
# Run tests for converted file
~/.claude/scripts/run-tests.sh -pl service -Dtest=ConvertedServiceTest

# Check for remaining anti-patterns
grep -l "\.apply\s*{" <file>
grep -l "lateinit var behandling\|lateinit var fagsak" <file>
```
