# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅ Completed]
  2. [✅ Completed]
  3. [✅ Completed]
  4. [✅ Completed]
  5. [✅ Completed]

### Phase 2 — getting context:
  1. [✅ Completed]
  2. [✅ Completed]

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅ Completed]
  2. [✅ Completed]
  3. [✅ Completed]
  4. [✅ Completed]

### Phase 4 — finalizing the review
  1. [✅ Completed]
  2. [✅ Completed]
  3. [✅ Completed]

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Applied to 6 object initializations with multiple property assignments for cleaner code.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No Behandling or Fagsak builders found in this test file.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already have good AAA separation with blank lines.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No single-return functions that would benefit from expression body.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Companion object is already correctly placed at the end of the class.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backtick syntax for readability.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using MockK throughout the file.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers throughout.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations are properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No groups of assertions on same object found that would benefit from run.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Imports are already correct for MockK and Kotest.

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