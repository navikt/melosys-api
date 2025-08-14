# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅ Completed] Requirements understood
  2. [✅ Completed] Read tracking file
  3. [✅ Completed] Created progress file
  4. [✅ Completed] Updated tracking file
  5. [✅ Completed] Set status to Processing

### Phase 2 — getting context:
  1. [✅ Completed] Read kotlin-test-file-processing-rules.md
  2. [✅ Completed] Read all three files (Kotlin, Java, main class)

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅ Completed] First pass - systematic identification
  2. [✅ Completed] Second pass - fix all issues
  3. [✅ Completed] Verification - tests pass
  4. [✅ Completed] All rules applied

### Phase 4 — finalizing the review
  1. [✅ Completed] Validation checklist completed
  2. [In Progress] Update tracking file
  3. [⏳] Commit changes

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Code already correctly uses apply for object initialization in all helper methods.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Fixed 3 occurrences of BehandlingTestFactory.builderWithDefaults() converted to Behandling.forTest {} DSL pattern. Added import for forTest DSL.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already have good AAA structure with proper spacing.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No violations found - helper functions already use appropriate syntax.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Companion object is already correctly placed at the end of the class.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: All test methods already use backticks for readable test names.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already fully migrated to MockK with correct imports and usage.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers throughout the test file.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations (@Test, @BeforeEach) are properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No violations found - assertions are already appropriately structured.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added import for no.nav.melosys.domain.forTest DSL. All imports are correct.

## Application Instructions
1. Preserve original test logic and assertions
2. Add appropriate Kotlin annotations and modifiers
3. Ensure proper import statements

## Validation Checklist
- [✅] All tests compile successfully
- [✅] All tests pass (8 tests, 0 failures, 0 errors)
- [✅] The Golden Steps are followed
- [✅] No regression in test coverage
- [✅] Kotlin-specific features are properly utilized
- [✅] Code follows team's Kotlin style guide
- [✅] MockK and Kotest imports are correct
- [✅] JUnit annotations are preserved
- [✅] Companion objects are placed at the bottom of the class
- [✅] Test factories use Kotlin DSL where available