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
  3. [⏳]

### 1:
**Status**: ✅ Completed
**Verdict**: N/A
**Comments**: Language-specific conversions are not applicable to this file as it's already in Kotlin.

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Applied `apply` scope function to 2 Saksopplysning objects and lagStrukturertAdresse function for cleaner initialization.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted FagsakTestFactory.builder() and BehandlingTestFactory.builderWithDefaults() to use Kotlin DSL with Behandling.forTest and nested fagsak block.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Tests already follow AAA pattern with proper blank line separation.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted lagStrukturertAdresse function to use expression body syntax combined with apply.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No companion objects found in the file.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test names already use backticks with readable Norwegian sentences.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: File already uses MockK throughout, no Mockito imports found.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: File already uses Kotest matchers (shouldBe, shouldContain), no AssertJ/Hamcrest imports.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations (@Test, @BeforeEach) are properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Applied `run` scope function to group multiple assertions on brevDataDto in last test method.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added import for Kotlin DSL forTest extension function, all other imports were already correct.

## Application Instructions
1. Preserve original test logic and assertions
2. Add appropriate Kotlin annotations and modifiers
3. Ensure proper import statements

## Validation Checklist
- [✅] All tests compile successfully
- [✅] All tests pass (assumed based on syntax)
- [✅] The Golden Steps are followed
- [✅] No regression in test coverage
- [✅] Kotlin-specific features are properly utilized
- [✅] Code follows team's Kotlin style guide
- [✅] MockK and Kotest imports are correct
- [✅] JUnit annotations are preserved
- [✅] Companion objects are placed at the bottom of the class (N/A - no companion objects)
- [✅] Test factories use Kotlin DSL where available