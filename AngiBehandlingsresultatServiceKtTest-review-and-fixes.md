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
**Verdict**: Unchanged
**Comments**: Existing apply blocks are appropriate for non-data classes (Behandlingsresultat, Medlemskapsperiode).

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Migrated BehandlingTestFactory and FagsakTestFactory to Behandling.forTest with Kotlin DSL.

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added two blank lines between Arrange-Act-Assert sections in all test methods.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: No functions with single return statements that would benefit from expression body syntax.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Moved companion object from middle of class to the bottom.

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted all test names from underscore style to backticks with Norwegian descriptions.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using MockK throughout the file.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers (shouldBe, shouldContain, shouldThrow).

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations preserved (@Test, @BeforeEach, @ExtendWith).

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Single assertions kept as-is; no groups of related assertions on same object found.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Updated imports to include forTest and fagsak extension functions.

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