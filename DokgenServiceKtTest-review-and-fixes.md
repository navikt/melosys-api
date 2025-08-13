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
**Verdict**: N/A - file is already in Kotlin
**Comments**: File is already a Kotlin test file with good structure.

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: File already uses apply correctly for object initialization throughout.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted BehandlingTestFactory.builderWithDefaults() to Behandling.forTest DSL syntax.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already follow AAA pattern with clear separation between sections.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted lagBehandling() helper method to use expression body syntax.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No companion object in this test class.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: All test methods already use backticks with descriptive Norwegian names.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: File already uses MockK throughout, no Mockito usage found.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: File already uses Kotest matchers exclusively.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations (@Test, @BeforeEach) are preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added run blocks to group multiple assertions on brevbestilling and brevbestillingSlot objects.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added import for Behandling.forTest DSL.

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
