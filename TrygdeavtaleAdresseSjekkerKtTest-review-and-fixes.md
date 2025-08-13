# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅ Completed] Read and understood requirements
  2. [✅ Completed] Read tracking file and identified file 67
  3. [✅ Completed] Created TrygdeavtaleAdresseSjekkerKtTest-review-and-fixes.md
  4. [✅ Completed] Updated tracking file with ProgressFile
  5. [✅ Completed] Set status to "Processing"

### Phase 2 — getting context:
  1. [✅ Completed] Read kotlin-test-file-processing-rules.md
  2. [✅ Completed] Read and understand test files and main class

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅ Completed] First Pass - Systematic Identification
  2. [✅ Completed] Second Pass - Fix All Issues
  3. [✅ Completed] Verification
  4. [✅ Completed] Run tests to ensure they pass

### Phase 4 — finalizing the review
  1. [✅ Completed] Follow Validation Checklist
  2. [⏳ Not started] Update tracking file
  3. [⏳ Not started] Commit changes

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already correctly using apply in lagLovvalgsperiode() function.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No Behandling or Fagsak builders used in this test file.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added two blank lines between arrange, act, and assert sections in all test methods.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted kodeEllerNull() and lagPersonAdresse() to expression body syntax.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Moved companion object from beginning to end of class per Kotlin best practices.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backticks correctly.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No mocking needed in this test file.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers correctly.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No multiple assertions on same object to group.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Imports already correct, using Kotest matchers.

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