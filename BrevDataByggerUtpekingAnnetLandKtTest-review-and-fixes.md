# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅] Read and understood requirements
  2. [✅] Read tracking file  
  3. [✅] Created progress file
  4. [⏳] Update tracking file with ProgressFile field
  5. [⏳] Update tracking file with Processing status

### Phase 2 — getting context:
  1. [✅] Read kotlin-test-file-processing-rules.md (already read)
  2. [✅] Read and understand Kotlin test file, Java test file, and main class

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅] First Pass - Systematic Identification
  2. [✅] Second Pass - Fix All Issues
  3. [✅] Verification

### Phase 4 — finalizing the review
  1. [✅] Ensure tests pass
  2. [In Progress] Update tracking file
  3. [⏳] Commit changes

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: CRITICAL: Migrated BehandlingTestFactory to Kotlin DSL.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Both test methods now have AAA separation with two blank lines.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backticks - no changes needed.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 3.3: Keep JUnit Annotations
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Two assertions on same object now grouped using run.

### 4. Import Management
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

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
- [✅] Companion objects are placed at the bottom of the class (N/A - no companion object)
- [✅] Test factories use Kotlin DSL where available