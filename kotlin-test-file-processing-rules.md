# Kotlin Test File Processing Rules

## IMPORTANT: STRICT COMPLIANCE REQUIRED
**ALL RULES ARE MANDATORY AND MUST BE FOLLOWED WITHOUT EXCEPTION**
- If a rule cannot be applied due to technical constraints, STOP and ask for clarification
- Do NOT make pragmatic decisions to "keep things working" if it violates a rule
- Look for existing examples in the codebase before making assumptions
- If unsure, STOP and ask

## Critical Implementation Notes (Learned from Experience)

### Spring WebMvcTest with MockK
For tests using `@WebMvcTest`:
1. **MUST use `@MockkBean` from `com.ninjasquad.springmockk`** - NOT `@MockBean` and NOT standard MockK `@MockK`
2. Import: `import com.ninjasquad.springmockk.MockkBean`
3. The `springmockk` dependency is already available in frontend-api/pom.xml
4. Example from codebase: `frontend-api/src/test/kotlin/no/nav/melosys/tjenester/gui/fagsaker/FagsakControllerTest.kt`
5. **MockK is stricter than Mockito**: You must mock ALL method calls explicitly
   - Common pattern: `aksesskontroll.autoriser()` often needs explicit mocking
   - Check the controller implementation to identify all service method calls

### MockK Syntax for Spring Tests
```kotlin
// Correct imports for Spring WebMvcTests
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify

// Correct annotation
@MockkBean
private lateinit var service: MyService

// Correct mocking syntax
every { service.method(any()) } returns result
```

### MockMvc Assertions - EXCEPTION TO RULE 3.2
**EXCEPTION**: For MockMvc assertions, keep Hamcrest matchers ONLY for jsonPath assertions:
- MockMvc's `andExpect()` requires `ResultMatcher` types that only Hamcrest provides
- Keep: `import org.hamcrest.Matchers.equalTo` (and other needed Hamcrest matchers)
- Keep: `.andExpect(jsonPath("$...", equalTo(value)))`
- This is the ONLY exception where Hamcrest is allowed
- All other assertions outside of MockMvc chains should use Kotest

### Do NOT Use
- `@MockBean` - This is for Mockito only
- `org.mockito.kotlin.*` - We're using MockK, not mockito-kotlin
- Mockito imports of any kind

## Rule Categories

### 1. Language-Specific Conversions
**Pattern:** [Describe what to look for]
**Issue:** [What's wrong with the current state]
**Fix:** [How to correct it]
**Example:**
```kotlin
// Before (problematic)
[code example]

// After (corrected)
[code example]
```

### 2. Kotlin Language Best Practices

#### Rule 2.1: Use `apply` for Object Initialization
**Pattern:** Variable declaration followed by multiple property assignments on the same object
**Issue:** Java-style object initialization that doesn't leverage Kotlin's scope functions
**Fix:** Convert to use `apply` scope function for cleaner, more idiomatic Kotlin code
**Priority:** Medium
**Example:**
```kotlin
// Before (Java-style)
val saksopplysning = Saksopplysning()
saksopplysning.dokument = sedDokument
saksopplysning.type = SaksopplysningType.SEDOPPL
saksopplysning.status = Status.ACTIVE

// After (Kotlin idiomatic)
val saksopplysning = Saksopplysning().apply {
    dokument = sedDokument
    type = SaksopplysningType.SEDOPPL
    status = Status.ACTIVE
}
```

**Exception for Data Classes:**
```kotlin
// For Kotlin data classes, prefer named arguments over apply
// Before (apply on data class - avoid this)
val person = Person().apply {
    name = "John"
    age = 30
    email = "john@example.com"
}

// After (named arguments for data classes)
val person = Person(
    name = "John",
    age = 30,
    email = "john@example.com"
)
```

**Application Rules:**
- Only apply when there are 2+ property assignments
- **Do NOT use `apply` for Kotlin data classes** - use named arguments instead
- Preserve any method calls that return values (don't convert those to `apply`)
- Consider `also` if you need to access the object reference within the block

#### Rule 2.2: Migrate Behandling and Fagsak Builders to Kotlin DSL
**Pattern:** Java builder pattern usage with `BehandlingTestFactory.builder()` or `FagsakTestFactory.builder()`
**Issue:** Using Java-style builders instead of Kotlin DSL for Behandling and Fagsak test objects
**Fix:** Convert to Kotlin DSL using `.forTest` functions for these domain objects
**Priority:** HIGH - MANDATORY
**CRITICAL:** BOTH `Behandling.forTest` AND `Fagsak.forTest` exist and MUST be used!

**Example:**
```kotlin
// WRONG - NEVER DO THIS:
val fagsak = FagsakTestFactory.builder().medVirksomhet().build()
val behandling = BehandlingTestFactory.builderWithDefaults()
    .medTema(Behandlingstema.YRKESAKTIV)
    .medType(Behandlingstyper.FØRSTEGANG)
    .medFagsak(fagsak)
    .build()

// CORRECT - ALWAYS DO THIS:
val behandling = Behandling.forTest {
    tema = Behandlingstema.YRKESAKTIV
    type = Behandlingstyper.FØRSTEGANG
    fagsak {  // Extension function in Behandling.Builder
        medVirksomhet()  // Call builder methods inside the DSL block
        gsakSaksnummer = 123456789L
    }
}
val fagsak = behandling.fagsak

// Also CORRECT - when you need a standalone Fagsak:
val fagsak = Fagsak.forTest {
    medBruker()
    gsakSaksnummer = 123456789L
}
```

**Migration Rules:**
- **ALWAYS** replace `BehandlingTestFactory.builder()...build()` with `Behandling.forTest { }`
- **ALWAYS** replace `FagsakTestFactory.builder()...build()` with `Fagsak.forTest { }`
- **NEVER** assume DSL doesn't exist just because TestFactory exists - CHECK FIRST!
- Import the DSL functions: `import no.nav.melosys.domain.forTest`
- Convert `.medProperty(value)` to `property = value` inside the DSL block
- For builder methods like `medBruker()` or `medVirksomhet()`, call them inside the DSL block
- For nested fagsak in Behandling, use the `fagsak { }` extension function
- Other test factories (NOT Behandling/Fagsak) may continue using builder patterns if DSL is not available

#### Rule 2.3: Structure Tests with AAA Pattern
**Pattern:** Test methods without clear structure or separation
**Issue:** Test code lacks clear organization making it harder to understand and maintain
**Fix:** Organize tests using Arrange-Act-Assert pattern with two blank lines between sections
**Priority:** Medium
**Example:**
```kotlin
// Before (unstructured)
@Test
fun testCreateUser() {
    val repository = mockk<UserRepository>()
    val service = UserService(repository)
    val userData = UserData("John", "john@test.com")
    every { repository.save(any()) } returns User(1, "John", "john@test.com")
    val result = service.createUser(userData)
    result.id shouldBe 1
    result.name shouldBe "John"
    verify { repository.save(any()) }
}

// After (clean AAA structure)
@Test
fun testCreateUser() {
    val repository = mockk<UserRepository>()
    val service = UserService(repository)
    val userData = UserData("John", "john@test.com")
    every { repository.save(any()) } returns User(1, "John", "john@test.com")


    val result = service.createUser(userData)


    result.run {
        id shouldBe 1
        name shouldBe "John"
    }
    verify { repository.save(any()) }
}
```

**Application Rules:**
- Use two blank lines between Arrange, Act, and Assert sections
- Do NOT add section comments - let the structure speak for itself
- Keep each section focused on its purpose
- Only apply when natural/possible - don't force it for simple tests
- Combine with other rules (use `run` for grouped assertions in Assert section)

#### Rule 2.5: Use Expression Body When Possible
**Pattern:** Functions with single return statements using block body
**Issue:** Verbose function syntax when a simple expression would suffice
**Fix:** Convert to expression body syntax for cleaner, more concise code
**Priority:** Low
**Example:**
```kotlin
// Before (block body)
fun getUserName(user: User): String {
    return user.firstName + " " + user.lastName
}

fun isValidUser(user: User): Boolean {
    return user.age >= 18 && user.email.isNotEmpty()
}

fun createDefaultUser(): User {
    return User("John", "Doe", 25, "john@example.com")
}

// After (expression body)
fun getUserName(user: User): String = user.firstName + " " + user.lastName

fun isValidUser(user: User): Boolean = user.age >= 18 && user.email.isNotEmpty()

fun createDefaultUser(): User = User("John", "Doe", 25, "john@example.com")
```

**Application Rules:**
- Only apply to functions with a single return statement
- Keep block body for complex expressions or multiple statements
- Particularly useful for test helper functions and simple getters
- Can omit return type when it can be inferred

#### Rule 2.6: Move Companion Objects to End of Class
**Pattern:** `companion object` declarations at the beginning or middle of class OR constants not in companion object
**Issue:** Not following Kotlin coding conventions for companion object placement
**Fix:** Move companion objects to the end of the class definition and place constants inside them
**Priority:** Medium
**Note:** This is a Kotlin best practice - companion objects MUST be placed at the bottom of the class

#### Rule 2.7: Use Backticks for Readable Test Names (Optional)
**Pattern:** Test method names using underscores like `methodName_condition_expectedResult`
**Issue:** Test names can be hard to read with underscores
**Fix:** Convert to Kotlin's backtick syntax for more human-readable test names (keep Norwegian language)
**Priority:** Low (Optional - both styles are acceptable)
**Example:**
```kotlin
// Before (underscore style - still valid)
@Test
fun slettKontaktopplysning_kallerDeleteByIdMedGittSaksnummerOgOrgNummer() {
    // test code
}

@Test
fun avklarUtenlandskMyndighetSomAktørOgLagre_kasterFunksjonellException_nårDetErFlereLandkoder() {
    // test code
}

@Test
fun lagUtenlandskeMyndigheterFraBehandling_svelgerIkkeFunnetException_nårLandvelgerIkkeFinnerUtenlandskMyndighet() {
    // test code
}

// After (backtick style - more human readable)
@Test
fun `slettKontaktopplysning skal kalle deleteById med gitt saksnummer og orgnummer`() {
    // test code
}

@Test
fun `avklarUtenlandskMyndighetSomAktørOgLagre skal kaste FunksjonellException når det er flere landkoder`() {
    // test code
}

@Test
fun `lagUtenlandskeMyndigheterFraBehandling skal catche IkkeFunnetException når landvelger ikke finner utenlandsk myndighet`() {
    // test code
}

// Real example from codebase:
@Test
fun `opprettNyÅrsavregning skal lage ny årsavregning når det ikke finnes avregning`() {
    // test code
}
```

**Application Rules:**
- This is OPTIONAL - both styles are acceptable
- Write test names as natural Norwegian sentences
- Use "skal" (should) to describe expected behavior
- Keep the Norwegian language in test names
- Particularly useful for long, descriptive test names
- Makes tests read like specifications or requirements
**Example:**
```kotlin
// Before (companion object at beginning/middle)
class MyTest {
    companion object {
        const val TEST_VALUE = "test"
        @JvmStatic
        fun createTestData() = TestData()
    }

    @MockK
    private lateinit var repository: Repository

    @Test
    fun testSomething() {
        // test code
    }
}

// After (companion object at end)
class MyTest {
    @MockK
    private lateinit var repository: Repository

    @Test
    fun testSomething() {
        // test code
    }

    companion object {
        const val TEST_VALUE = "test"
        @JvmStatic
        fun createTestData() = TestData()
    }
}
```

**Application Rules:**
- Always place companion objects as the last element in the class
- Preserve all content within the companion object unchanged
- Follow Kotlin style guide conventions

### 3. Test Framework Migration

#### Rule 3.1: Migrate from Mockito to MockK
**Pattern:** Mockito imports and usage patterns
**Issue:** Using Java-based Mockito instead of Kotlin-friendly MockK
**Fix:** Replace Mockito with MockK syntax and imports
**Priority:** High
**Example:**
```kotlin
// Before (Mockito)
import org.mockito.Mockito.*
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MyTest {
    @Mock
    private lateinit var repository: Repository

    @Test
    fun testSomething() {
        `when`(repository.findById(1)).thenReturn(entity)
        verify(repository).findById(1)
    }
}

// After (MockK)
import io.mockk.*
import io.mockk.junit5.MockKExtension

@ExtendWith(MockKExtension::class)
class MyTest {
    @MockK
    private lateinit var repository: Repository

    @Test
    fun testSomething() {
        every { repository.findById(1) } returns entity
        verify { repository.findById(1) }
    }
}
```

#### Rule 3.2: Replace AssertJ/Hamcrest with Kotest Matchers
**Pattern:** `assertThat()` calls and hamcrest/assertj imports
**Issue:** Using Java assertion libraries instead of Kotlin-idiomatic Kotest matchers
**Fix:** Convert to Kotest matchers syntax
**Priority:** High
**EXCEPTION:** Keep Hamcrest ONLY for MockMvc jsonPath assertions (see "MockMvc Assertions - EXCEPTION TO RULE 3.2" section above)
**Example:**
```kotlin
// Before (AssertJ/Hamcrest)
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*

@Test
fun testSomething() {
    assertThat(result).isNotNull()
    assertThat(result.size).isEqualTo(3)
    assertThat(result, hasItem("expected"))
}

// After (Kotest)
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain

@Test
fun testSomething() {
    result shouldNotBe null
    result.size shouldBe 3
    result shouldContain "expected"
}
```

#### Rule 3.3: Keep JUnit Annotations
**Pattern:** JUnit test annotations
**Issue:** N/A - These should be preserved
**Fix:** Keep `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, etc.
**Priority:** N/A
**Example:**
```kotlin
// Keep these JUnit annotations
@Test
@BeforeEach
@AfterEach
@ParameterizedTest
@ValueSource
// etc.
```

#### Rule 3.4: Group Related Assertions with `run`
**Pattern:** Multiple assertions on the same object
**Issue:** Repetitive object references in assertion chains
**Fix:** Use `run` scope function to group related assertions on the same object
**Priority:** Medium
**Example:**
```kotlin
// Before (repetitive object references)
dokgenBrevbestillingRequest.bestillersId shouldBe "Z123456"
dokgenBrevbestillingRequest.mottaker shouldBe BRUKER
dokgenBrevbestillingRequest.fritekst shouldBe "henlagt sak fritekst"
dokgenBrevbestillingRequest.begrunnelseKode shouldBe "ANNET"

// After (grouped with run)
dokgenBrevbestillingRequest.run {
    bestillersId shouldBe "Z123456"
    mottaker shouldBe BRUKER
    fritekst shouldBe "henlagt sak fritekst"
    begrunnelseKode shouldBe "ANNET"
}

// Exception: Keep single assertions as-is
dokgenBrevbestillingRequest.bestillersId shouldBe "Z123456" // Don't convert single assertions
```

**Application Rules:**
- Only apply when there are 2+ assertions on the same object
- Keep single assertions without `run` for readability
- Can be combined with other Kotest matchers

### 4. Import Management

**Remove these imports:**
```kotlin
import org.mockito.*
import org.assertj.core.api.Assertions.*
import org.hamcrest.*
```

**Add these imports:**
```kotlin
import io.mockk.*
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.string.*
```

## Priority Levels
- **High Priority:** Critical issues that prevent compilation or cause runtime errors (Mockito/AssertJ migrations)
- **Medium Priority:** Issues that impact code quality or maintainability (apply/run patterns)
- **Low Priority:** Style improvements and minor optimizations

## Application Instructions
1. Process files in batches of 10-15 files
2. Apply rules in order of priority (High → Medium → Low)
3. Preserve original test logic and assertions
4. Maintain backward compatibility where possible
5. Add appropriate Kotlin annotations and modifiers
6. Ensure proper import statements

## Validation Checklist
- [ ] All tests compile successfully
- [ ] All tests pass
- [ ] No regression in test coverage
- [ ] Kotlin-specific features are properly utilized
- [ ] Code follows team's Kotlin style guide
- [ ] MockK and Kotest imports are correct
- [ ] JUnit annotations are preserved
- [ ] Companion objects are placed at the bottom of the class
- [ ] Test factories use Kotlin DSL where available
