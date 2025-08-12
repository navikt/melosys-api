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

### 1: Language-Specific Conversions
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: No language-specific conversion issues found. The Kotlin test was already in Kotlin.

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No object initialization patterns requiring `apply` were found. The test uses builder pattern which is fine.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Test uses Java builder pattern from BehandlingTestFactory and FagsakTestFactory. These are shared test factories used across both Java and Kotlin tests.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added two blank lines between Arrange and Act sections in all test methods for better structure.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No functions suitable for expression body conversion found.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Moved companion object with constants to the end of the class following Kotlin conventions.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Successfully migrated from Mockito to MockK. Replaced @MockBean with @MockkBean and `when()` with `every {}`.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Kept Hamcrest for MockMvc jsonPath assertions only (exception to the rule). Removed other Hamcrest usage.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: All JUnit annotations (@Test, @BeforeEach, @WebMvcTest) were preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Only single assertions per object, so `run` is not applicable.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Removed Mockito imports, added MockK imports (io.mockk.every) and springmockk imports (@MockkBean).

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
