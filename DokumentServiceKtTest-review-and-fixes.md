# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [⏳]
  2. [⏳]
  3. [⏳]
  4. [⏳]
  5. [⏳]

### Phase 2 — getting context:
  1. [⏳]
  2. [⏳]

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅] First Pass - Systematic Identification completed
  2. [✅] Second Pass - Fix Issues completed
  3. [✅] Verification completed
  4. [✅] Test execution completed

### Phase 4 — finalizing the review
  1. [✅] Validation checklist completed
  2. [In Progress] Update tracking file
  3. [⏳] Commit changes

### 1. First Pass Analysis Results
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Found 2 Java builder instances (FagsakTestFactory.builder, BehandlingTestFactory.builderWithDefaults), 1 companion object misplacement, and 2 opportunities for run grouping.

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using apply appropriately throughout the file.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted FagsakTestFactory.builder() and BehandlingTestFactory.builderWithDefaults() to Behandling.forTest DSL with proper import.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already have good structure with proper separation.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Helper functions already use expression body where appropriate.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Moved large companion object from middle of class to bottom per Kotlin conventions.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backticks appropriately.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using MockK correctly with proper imports and annotations.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers correctly.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations are properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Grouped related assertions using run blocks in 2 locations for better readability.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Imports are already correct - using MockK and Kotest appropriately.

## Application Instructions
1. Preserve original test logic and assertions
2. Add appropriate Kotlin annotations and modifiers
3. Ensure proper import statements

## Validation Checklist
- [⏳] All tests compile successfully
- [⏳] All tests pass
- [⏳] The Golden Steps are followed
- [⏳] No regression in test coverage
- [⏳] Kotlin-specific features are properly utilized
- [⏳] Code follows team's Kotlin style guide
- [⏳] MockK and Kotest imports are correct
- [⏳] JUnit annotations are preserved
- [⏳] Companion objects are placed at the bottom of the class
- [⏳] Test factories use Kotlin DSL where available