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
  1. [✅]
  2. [✅]
  3. [✅]
  4. [✅]

### Phase 4 — finalizing the review
  1. [✅]
  2. [⏳]
  3. [⏳]

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using `apply` correctly in helper methods.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No Behandling or Fagsak builders present in this test.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests are simple one-liners, AAA pattern not applicable.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Helper functions use `apply` blocks, expression body not suitable.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No companion object present in this test class.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted all 12 test method names from underscore style to backtick style with descriptive Norwegian names.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No mocking used in this test file.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers (shouldBe).

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations (@Test) preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Each test has single assertion, grouping not needed.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Imports are clean, using Kotest already.

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
- [✅] Companion objects are placed at the bottom of the class
- [✅] Test factories use Kotlin DSL where available