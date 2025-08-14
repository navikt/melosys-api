# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅]
  2. [✅]
  3. [✅]
  4. [✅]
  5. [✅]

### Phase 2 — getting context:
  1. [✅]
  2. [✅]

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅] First Pass - Identified 2 main issues (Behandling/Fagsak DSL migration, test name improvements)
  2. [✅] Second Pass - Fixed all identified issues
  3. [✅] Verification - Tests compile and pass successfully
  4. [✅]

### Phase 4 — finalizing the review
  1. [✅]
  2. [✅]
  3. [⏳]

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using apply correctly in the file (lines 77, 166, 228, 233)

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Migrated BehandlingTestFactory.builderWithDefaults() to Behandling.forTest and FagsakTestFactory.lagFagsak() to Fagsak.forTest

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already well-structured with clear AAA separation

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Not applicable - functions have multiple statements or complex logic

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No companion objects in the file

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Improved all 8 test names to use Norwegian sentences with "skal" pattern for better readability

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using MockK correctly with @RelaxedMockK annotations

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers (shouldContainExactly)

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations preserved (@Test, @BeforeEach)

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Only single assertions in tests, no grouping needed

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added import for no.nav.melosys.domain.forTest for DSL functions

## Application Instructions
1. Preserve original test logic and assertions
2. Add appropriate Kotlin annotations and modifiers
3. Ensure proper import statements

## Validation Checklist
- [✅] All tests compile successfully
- [✅] All tests pass
- [✅] The Golden Steps are followed
- [✅] No regression in test coverage
- [✅] Kotlin-specific features are properly utilized
- [✅] Code follows team's Kotlin style guide
- [✅] MockK and Kotest imports are correct
- [✅] JUnit annotations are preserved
- [✅] Companion objects are placed at the bottom of the class (N/A - no companion objects)
- [✅] Test factories use Kotlin DSL where available