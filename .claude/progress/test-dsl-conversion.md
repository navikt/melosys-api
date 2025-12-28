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
| **Total** | **15** | **204** |

### New Test Factories Created
- `VilkaarsresultatTestFactory` - Created during Phase 1 service/behandling conversion
- Extended `BehandlingsresultatTestFactory` with `vilkaarsresultat {}` DSL builder
- Extended `SaksopplysningTestFactory` with `kilder` property

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
- [ ] service/dokument/** - 25 files
- [ ] service/eessi/** - 8 files
- [ ] service/sak/** - 12 files
- [ ] service/vedtak/** - 6 files

### Phase 2: Saksflyt Tests
- [ ] saksflyt/steg/behandling/** - 12 files
- [ ] saksflyt/steg/sed/** - 15 files
- [ ] saksflyt/steg/brev/** - 10 files
- [ ] saksflyt/steg/medl/** - 5 files

### Phase 3: Frontend-API Tests
- [ ] frontend-api/tjenester/gui/** - 15 files

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
