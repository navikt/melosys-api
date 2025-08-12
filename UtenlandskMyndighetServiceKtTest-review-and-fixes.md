# Kotlin Test File Review Processing Template

Possible statuses: `⏳ Not started`, `In Progress`, `✅ Completed`
Possible verdicts: `Changed`, `Unchanged`
Comments: Should be max 3–4 sentences long, providing context or reasoning for the status.

# Overall progress
## The Golden Steps
### Phase 1 — starting the job:
  1. [✅] Read and understood requirements-java-kotlin-review-and-fixes.md
  2. [✅] Read tracking-java-kotlin-review-and-fixes.md
  3. [✅] Created UtenlandskMyndighetServiceKtTest-review-and-fixes.md
  4. [✅] Write the name of the file in tracking
  5. [✅] Write the status of the file in tracking

### Phase 2 — getting context:
  1. [✅] Read kotlin-test-file-processing-rules.md
  2. [✅] Read and understand the Kotlin test file, Java test file, and main class

### Phase 3 — reviewing and fixing the Kotlin test file
  1. [✅] Check if the Kotlin test file follows the rules
  2. [✅] Fill out status, verdict, and comments for each rule
  3. [✅] Make necessary changes to the Kotlin test file
  4. [✅] Ensure tests are running and passing

### Phase 4 — finalizing the review
  1. [✅] Complete the review and make necessary changes
  2. [⏳] Update tracking-java-kotlin-review-and-fixes.md
  3. [⏳] Commit changes

### 1:
**Status**: ⏳ Not started
**Verdict**:
**Comments**:

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already correctly using apply for UtenlandskMyndighet initialization.

#### Rule 2.2: Migrate from Java Builders to Kotlin DSL
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Converted Behandling and Fagsak builders to use Kotlin DSL (.forTest).

#### Rule 2.3: Structure Tests with AAA Pattern
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added AAA pattern structure with two blank lines between sections.

#### Rule 2.4: Use Expression Body When Possible
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Not applicable - no single-return functions to convert.

#### Rule 2.5: Move Companion Objects to End of Class
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Companion object already correctly placed at the end of the class.

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using MockK with proper annotations and imports.

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Already using Kotest matchers throughout.

#### Rule 3.3: Keep JUnit Annotations
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: JUnit annotations properly preserved.

#### Rule 3.4: Group Related Assertions with `run`
**Status**: ✅ Completed
**Verdict**: Unchanged
**Comments**: Not needed - assertions are already well structured.

### 4. Import Management
**Status**: ✅ Completed
**Verdict**: Changed
**Comments**: Added import for Kotlin DSL (forTest).

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