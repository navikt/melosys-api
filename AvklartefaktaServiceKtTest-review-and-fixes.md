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
  2. [✅]
  3. [✅]

### 1:
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No language-specific conversion issues found. Test is already properly written in Kotlin.

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already uses `apply` pattern correctly in multiple test methods.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No Behandling or Fagsak builders present in this test.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added two blank lines between Arrange, Act, and Assert sections in 8 test methods for better readability.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No functions with single return statements to convert.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Companion object is already correctly placed at the bottom of the class.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backticks throughout the file.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already uses MockK throughout with proper annotations and syntax.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already uses Kotest matchers throughout. No AssertJ or Hamcrest imports present.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations (@Test, @BeforeEach) are properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added `run` blocks to group related assertions in 3 test methods for better readability.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Imports are already clean with MockK and Kotest matchers. No Mockito, AssertJ, or Hamcrest imports to remove.

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