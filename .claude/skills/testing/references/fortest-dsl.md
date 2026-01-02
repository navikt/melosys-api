# forTest DSL Reference

Type-safe Kotlin DSL for creating test data with sensible defaults and nested entity support.

## Table of Contents

- [Core Pattern](#core-pattern)
- [Available TestFactory Classes](#available-testfactory-classes)
- [Nested Entity Building](#nested-entity-building)
- [Behandlingsresultat with Periods](#behandlingsresultat-with-periods)
- [Test Base Class Patterns](#test-base-class-patterns)
- [Fagsak with Multiple Behandlinger](#fagsak-with-multiple-behandlinger)
- [@MelosysTestDsl Annotation](#melosystestdsl-annotation)
- [Key Imports](#key-imports)
- [Standalone Functions](#standalone-functions)
- [Helper Method Patterns](#helper-method-patterns)
- [Composite DSL Pattern](#composite-dsl-pattern)
- [Saksopplysning Document Types](#saksopplysning-document-types)
- [Dual-Mode Builder Pattern](#dual-mode-builder-pattern)
- [Inline DSL Refactoring Guide](#inline-dsl-refactoring-guide)
- [Anti-Patterns](#anti-patterns)

## Core Pattern

All domain entities use extension functions on their companion objects:

```kotlin
val fagsak = Fagsak.forTest {
    tema = Sakstemaer.TRYGDEAVGIFT
    type = Sakstyper.FTRL
}

val behandling = Behandling.forTest {
    status = Behandlingsstatus.AVSLUTTET
    tema = Behandlingstema.YRKESAKTIV
}
```

Each `forTest` function:
- Provides sensible defaults for all required fields
- Returns a fully constructed entity
- Automatically sets up bidirectional relationships

## Available TestFactory Classes

| Entity | Module | Factory | Location |
|--------|--------|---------|----------|
| `Fagsak` | domain | `FagsakTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Behandling` | domain | `BehandlingTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Behandlingsresultat` | domain | `BehandlingsresultatTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Behandlingsnotat` | domain | `BehandlingsnotatTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Medlemskapsperiode` | domain | `MedlemskapsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Lovvalgsperiode` | domain | `LovvalgsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Anmodningsperiode` | domain | `AnmodningsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Utpekingsperiode` | domain | `UtpekingsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Vilkaarsresultat` | domain | `VilkaarsresultatTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Avklartefakta` | domain | `AvklartefaktaTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Trygdeavgiftsperiode` | domain | `TrygdeavgiftsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/avgift/` |
| `MottatteOpplysninger` | domain | `MottatteOpplysningerTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `Soeknad` | domain | `SoeknadTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `AnmodningEllerAttest` | domain | `AnmodningEllerAttestTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `Saksopplysning` | domain | `SaksopplysningTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `ArbeidsforholdDokument` | domain | `ArbeidsforholdDokumentBuilder` | `domain/src/test/kotlin/.../domain/` |
| `PersonDokument` | domain | `PersonDokumentTestFactory` | `domain/src/test/kotlin/.../dokument/` |
| `OrganisasjonDokument` | domain | `OrganisasjonDokumentTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Årsavregning` | domain | `ÅrsavregningTestFactory` | `domain/src/test/kotlin/.../domain/avgift/` |
| `HelseutgiftDekkesPeriode` | domain | `HelseutgiftDekkesPeriodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Prosessinstans` | saksflyt-api | `ProsessinstansTestFactory` | `saksflyt-api/src/test/kotlin/.../domain/` |

## Nested Entity Building

Build complex object graphs with nested DSL blocks:

```kotlin
val behandling = Behandling.forTest {
    fagsak {
        medBruker()
        medVirksomhet()
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
    }
    mottatteOpplysninger {
        type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        eksternReferanseID = "JOARK-12345"
        soeknad {
            landkoder("BE", "NL")
            bostedLandkode = "NO"
            bostedPoststed = "Oslo"
            fysiskeArbeidssted {
                landkode = "BE"
                poststed = "Brussel"
                virksomhetNavn = "Acme Corp"
            }
        }
    }
    saksopplysning {
        type = SaksopplysningType.PDL_PERSOPL
        personDokument {
            fnr = "12345678901"
            fornavn = "Test"
            etternavn = "Testesen"
            fødselsdato = LocalDate.of(1985, 6, 15)
        }
    }
    saksopplysning {
        type = SaksopplysningType.ORG
        organisasjonDokument {
            orgnummer = "987654321"
            navn = "Acme AS"
        }
    }
}
```

### Fagsak Helper Methods

```kotlin
Fagsak.forTest {
    medBruker()           // Adds aktør with BRUKER role
    medVirksomhet()       // Adds aktør with VIRKSOMHET role
    medTrygdemyndighet()  // Adds aktør with TRYGDEMYNDIGHET role
    medGsakSaksnummer()   // Sets default GSAK saksnummer
}
```

### AnmodningEllerAttest Data

```kotlin
val behandling = Behandling.forTest {
    fagsak {
        medBruker()
        type = Sakstyper.FTRL
    }
    mottatteOpplysninger {
        type = Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST
        anmodningEllerAttest {
            avsenderland = Land_iso2.SE
            lovvalgsland = Land_iso2.NO
        }
    }
}
```

## Behandlingsresultat with Periods

```kotlin
val behandlingsresultat = Behandlingsresultat.forTest {
    type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    behandling {
        fagsak { type = Sakstyper.FTRL }
    }
    vedtakMetadata {
        vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
    }
    medlemskapsperiode {
        fom = LocalDate.of(2023, 1, 1)
        tom = LocalDate.of(2023, 12, 31)
        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        trygdeavgiftsperiode {
            trygdesats = 6.8.toBigDecimal()
            grunnlagInntekstperiode {
                type = Inntektskildetype.ARBEIDSINNTEKT
                arbeidsgiversavgiftBetalesTilSkatt = false
                avgiftspliktigMndInntekt = Penger(15000.0)
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        }
    }
    lovvalgsperiode {
        fom = LocalDate.of(2024, 1, 1)
        tom = LocalDate.of(2024, 12, 31)
        lovvalgsland = Land_iso2.NO
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
    }
    årsavregning {
        aar = 2023
    }
    helseutgiftDekkesPeriode {
        fom = LocalDate.of(2023, 1, 1)
        tom = LocalDate.of(2023, 12, 31)
    }
    begrunnelse("BEGRUNNELSE_KODE")
}
```

## Test Base Class Patterns

Create reusable helper functions in test base classes:

```kotlin
abstract class ÅrsavregningServiceTestBase {

    // Factory method with customizable defaults
    fun lagTidligereBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        id = 1L
        type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        vedtakMetadata { vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK }
        init()  // Allow caller to customize
    }

    // Convenience extension for string dates
    protected fun BehandlingsresultatTestFactory.Builder.medlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        medTrygdeavgift: Boolean = true,
        init: MedlemskapsperiodeTestFactory.Builder.() -> Unit = {}
    ) {
        medlemskapsperioder.add(medlemskapsperiodeForTest {
            fom = LocalDate.parse(start)
            tom = LocalDate.parse(slutt)
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = innvilgelsesResultat
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            if (medTrygdeavgift) {
                trygdeavgiftsperiode(start, slutt)
            }
            init()
        })
    }

    // Nested helper for trygdeavgiftsperiode
    protected fun MedlemskapsperiodeTestFactory.Builder.trygdeavgiftsperiode(
        start: String,
        slutt: String,
        init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit = {}
    ) {
        trygdeavgiftsperiode {
            periodeFra = LocalDate.parse(start)
            periodeTil = LocalDate.parse(slutt)
            trygdeavgiftsbeløpMd = BigDecimal(5000.0)
            trygdesats = BigDecimal(3.5)
            grunnlagInntekstperiode {
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                avgiftspliktigMndInntekt = Penger(5000.0)
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
            init()
        }
    }
}
```

### Usage in Subclass Tests

```kotlin
class ÅrsavregningServiceOpprettTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `test with custom behandlingsresultat`() {
        val behandlingsresultat = lagTidligereBehandlingsresultat {
            medlemskapsperiode("2023-01-01", "2023-12-31")
            medlemskapsperiode("2024-01-01", "2024-06-30", medTrygdeavgift = false)
        }
        // ...
    }
}
```

## Fagsak with Multiple Behandlinger

```kotlin
val fagsak = Fagsak.forTest {
    tema = Sakstemaer.UNNTAK
    behandling {
        id = 1L
        type = Behandlingstyper.NY_VURDERING
        status = Behandlingsstatus.AVSLUTTET
    }
    behandling {
        id = 2L
        type = Behandlingstyper.ÅRSAVREGNING
        status = Behandlingsstatus.OPPRETTET
    }
}
// All behandlinger are automatically linked to fagsak
fagsak.behandlinger.forEach { it.fagsak == fagsak } // true
```

## Prosessinstans

```kotlin
val prosessinstans = Prosessinstans.forTest {
    id = UUID.fromString("da6a548b-59a8-4f19-9788-434254728307")
    behandling {
        fagsak {
            medBruker()
        }
    }
    medData(ProsessDataKey.SAKSBEHANDLER, "Z123456")
}
```

## @MelosysTestDsl Annotation

Builders are marked with `@MelosysTestDsl` to prevent scope pollution:

```kotlin
@DslMarker
annotation class MelosysTestDsl

@MelosysTestDsl
class Builder { ... }
```

This prevents accidentally accessing outer scope properties inside nested blocks:

```kotlin
// Without @MelosysTestDsl - could accidentally access outer `fom`
val behandlingsresultat = Behandlingsresultat.forTest {
    medlemskapsperiode {
        fom = ...  // Which fom? Ambiguous!
    }
}

// With @MelosysTestDsl - compiler error if accessing wrong scope
```

## Key Imports

```kotlin
// Domain entities - main forTest functions
import no.nav.melosys.domain.*
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.domain.lovvalgsperiodeForTest
import no.nav.melosys.domain.saksopplysningForTest

// Avgift
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.Penger

// Mottatte opplysninger
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.mottatteopplysninger.anmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest

// Saksflyt
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.domain.behandling
```

## Standalone Functions

Some entities use standalone functions instead of companion extensions:

```kotlin
val medlemskapsperiode = medlemskapsperiodeForTest {
    fom = LocalDate.of(2023, 1, 1)
    tom = LocalDate.of(2023, 12, 31)
    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
}

val lovvalgsperiode = lovvalgsperiodeForTest {
    fom = LocalDate.of(2024, 1, 1)
    tom = LocalDate.of(2024, 12, 31)
    lovvalgsland = Land_iso2.NO
}

val saksopplysning = saksopplysningForTest {
    type = SaksopplysningType.ORG
    organisasjonDokument {
        orgnummer = "987654321"
        navn = "Acme AS"
        sektorkode = "2100"
    }
}

val mottatteOpplysninger = mottatteOpplysningerForTest {
    type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
    soeknad {
        landkoder("NO", "SE")
    }
}
```

## Søknad with Business Data

The `SoeknadTestFactory` supports business-related data:

```kotlin
soeknadForTest {
    // Bosted address
    bostedAdresse(
        landkode = Landkoder.NO.kode,
        gatenavn = "Hjemmegata",
        husnummer = "23B",
        postnummer = "0165",
        poststed = "Oslo"
    )

    // Foreign businesses
    foretakUtland("123456789")
    foretakUtland("987654321")

    // Self-employed businesses
    selvstendigForetak("111222333")

    // Extra Norwegian employers
    ekstraArbeidsgiver("444555666")
}
```

## Anmodningsperiode

```kotlin
val anmodningsperiode = anmodningsperiodeForTest {
    unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
    unntakFraLovvalgsland = Land_iso2.SE
}
```

## Helper Method Patterns

### DSL Init Block Pattern (Preferred)

Helper methods should accept DSL init blocks instead of individual arguments:

```kotlin
// GOOD: Accept DSL init block
private fun lagBehandlingsresultat(
    init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
): Behandlingsresultat = Behandlingsresultat.forTest {
    id = BEHANDLING_ID
    behandling { fagsak { } }
    init()  // Apply caller customizations LAST
}

// Usage - clean DSL syntax
val result = lagBehandlingsresultat {
    type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    medlemskapsperiode { fom = LocalDate.now() }
}
```

```kotlin
// BAD: Arguments passed to forTest block
private fun lagBehandlingsresultat(
    id: Long = 1L,
    type: Behandlingsresultattyper = Behandlingsresultattyper.UNNTAK
): Behandlingsresultat = Behandlingsresultat.forTest {
    this.id = id
    this.type = type
}

// Usage - inconsistent with DSL pattern
val result = lagBehandlingsresultat(id = 5L, type = MEDLEM_I_FOLKETRYGDEN)
```

## Composite DSL Pattern

When tests need to configure multiple objects (e.g., behandling AND søknad), use a composite DSL with encapsulated build logic.

### Elegant Builder Pattern

```kotlin
/**
 * DSL builder for test setup.
 *
 * Example:
 * ```
 * lagBehandling {
 *     selvstendigForetakOrgnr = listOf(ORGNR1, ORGNR2)
 *     soeknad { bosted(Bosted()) }
 * }
 * ```
 */
@MelosysTestDsl
private class TestOppsettBuilder {
    var foretakUtlandOrgnr: String = ORGNR1
    var selvstendigForetakOrgnr: List<String> = emptyList()
    var ekstraArbeidsgivere: List<String> = emptyList()

    // Use existing factory builder types - not raw entity types!
    private var soeknadBlock: (SoeknadTestFactory.Builder.() -> Unit)? = null

    fun soeknad(init: SoeknadTestFactory.Builder.() -> Unit) {
        soeknadBlock = init
    }

    fun build(medlDokument: MedlemskapDokument, arbDokument: ArbeidsforholdDokument): TestOppsett {
        val builder = this
        val soeknad = soeknadForTest {
            // Defaults
            bostedAdresse(landkode = Landkoder.NO.kode, gatenavn = "HjemmeGata")
            foretakUtland(builder.foretakUtlandOrgnr)
            builder.selvstendigForetakOrgnr.forEach { selvstendigForetak(it) }
            builder.ekstraArbeidsgivere.forEach { ekstraArbeidsgiver(it) }
            // User customizations applied INSIDE the DSL block
            builder.soeknadBlock?.invoke(this)
        }

        val behandling = Behandling.forTest {
            fagsak { medBruker() }
            saksopplysning { dokument = medlDokument; type = SaksopplysningType.MEDL }
            saksopplysning { dokument = arbDokument; type = SaksopplysningType.ARBFORH }
            mottatteOpplysninger { mottatteOpplysningerData = soeknad }
        }

        return TestOppsett(behandling, arbDokument, soeknad)
    }
}

private data class TestOppsett(
    val behandling: Behandling,
    val arbDokument: ArbeidsforholdDokument,
    val soeknad: Soeknad
)

// Clean one-liner factory function
private fun lagBehandling(init: TestOppsettBuilder.() -> Unit = {}): TestOppsett {
    val medlDokument = MedlemskapDokument()
    val arbDokument = ArbeidsforholdDokument()
    lagArbeidsforhold(arbDokument, ORGNR2, LocalDate.of(2005, 1, 11), LocalDate.of(2017, 8, 11))

    return TestOppsettBuilder().apply(init).build(medlDokument, arbDokument)
}
```

### Key Design Principles

1. **Use existing factory builder types** - `SoeknadTestFactory.Builder`, not `Soeknad`
2. **Apply customizations INSIDE the DSL block** - No `.apply { }` after construction
3. **Encapsulate build logic in `build()` method** - Factory function stays clean
4. **`val builder = this`** - Capture reference for nested @MelosysTestDsl blocks

### Usage - All Configuration in DSL

```kotlin
@Test
fun `test with business data`() {
    val oppsett = lagBehandling {
        selvstendigForetakOrgnr = listOf(ORGNR1, ORGNR2)
    }
    // ...
}

@Test
fun `test edge case with empty bosted`() {
    // Configure søknad INSIDE the DSL - no post-construction mutation!
    val oppsett = lagBehandling {
        soeknad { bosted = Bosted() }
    }
    // ...
}

@Test
fun `test with custom behandling`() {
    val oppsett = lagBehandling {
        behandling { status = Behandlingsstatus.AVSLUTTET }
        soeknad { bosted.oppgittAdresse.gatenavn = "Custom Street" }
    }
    // ...
}
```

This pattern:
- All configuration happens inside the DSL block
- No post-construction mutation (anti-pattern!)
- Consistent DSL feel throughout
- Edge cases configured declaratively

## Saksopplysning Document Types

Saksopplysninger can hold different document types. Use DSL extensions for type-safe creation:

### PersonDokument

```kotlin
saksopplysning {
    type = SaksopplysningType.PDL_PERSOPL
    personDokument {
        fnr = "12345678901"
        fornavn = "Test"
        etternavn = "Testesen"
        fødselsdato = LocalDate.of(1985, 6, 15)
    }
}
```

### OrganisasjonDokument

```kotlin
saksopplysning {
    type = SaksopplysningType.ORG
    organisasjonDokument {
        orgnummer = "987654321"
        navn = "Acme AS"
        sektorkode = "2100"
    }
}
```

### SedDokument

```kotlin
saksopplysning {
    type = SaksopplysningType.SED_A001
    sedDokument {
        avsenderLandkode = Landkoder.SE
        rinaSaksnummer = "123456"
        rinaDokumentID = "doc-id-123"
        fnr = "12345678901"
    }
}
```

### ArbeidsforholdDokument

```kotlin
// Simple pattern - single arbeidsforhold with direct properties
saksopplysning {
    type = SaksopplysningType.ARBFORH
    arbeidsforholdDokument {
        arbeidsgiverID = "123456789"
        ansettelsesPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2023, 12, 31))
    }
}

// Multiple arbeidsforhold using nested blocks
saksopplysning {
    type = SaksopplysningType.ARBFORH
    arbeidsforholdDokument {
        arbeidsforhold {
            arbeidsgiverID = "111222333"
            ansettelsesPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
        }
        arbeidsforhold {
            arbeidsgiverID = "444555666"
            ansettelsesPeriode(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 31))
        }
    }
}
```

## Dual-Mode Builder Pattern

Some builders support two usage modes: simple (direct properties) and complex (nested blocks). This provides flexibility without requiring separate factory classes.

### Pattern Structure

```kotlin
@MelosysTestDsl
class DualModeBuilder {
    // Direct properties for simple case
    var simpleProperty: String? = null
    var anotherProperty: Long = 0

    // List for complex case
    private val itemList = mutableListOf<Item>()

    // Nested block function for complex case
    fun item(init: ItemBuilder.() -> Unit) {
        itemList.add(ItemBuilder().apply(init).build())
    }

    fun build(): Container = Container(
        if (itemList.isNotEmpty()) {
            itemList  // Complex mode: use nested blocks
        } else {
            listOf(Item(simpleProperty, anotherProperty))  // Simple mode: use direct properties
        }
    )
}
```

### ArbeidsforholdDokumentBuilder Example

The `ArbeidsforholdDokumentBuilder` uses this pattern:

```kotlin
@MelosysTestDsl
class ArbeidsforholdDokumentBuilder {
    // For single arbeidsforhold (simple pattern)
    var arbeidsforholdID: String? = null
    var arbeidsforholdIDnav: Long = 0
    var arbeidsgiverID: String? = null
    var ansettelsesPeriode: Periode? = null
    // ... other properties

    // For multiple arbeidsforhold
    private val arbeidsforholdListe = mutableListOf<Arbeidsforhold>()

    fun ansettelsesPeriode(fom: LocalDate, tom: LocalDate? = null) {
        ansettelsesPeriode = Periode(fom, tom)
    }

    fun arbeidsforhold(init: ArbeidsforholdBuilder.() -> Unit) {
        arbeidsforholdListe.add(ArbeidsforholdBuilder().apply(init).build())
    }

    fun build(): ArbeidsforholdDokument = ArbeidsforholdDokument(
        if (arbeidsforholdListe.isNotEmpty()) {
            arbeidsforholdListe
        } else {
            listOf(Arbeidsforhold().apply { /* copy properties */ })
        }
    )
}
```

### When to Use Each Mode

| Mode | Use When |
|------|----------|
| Simple (direct properties) | Single item, most tests |
| Complex (nested blocks) | Multiple items, specific combinations |

```kotlin
// Simple: most common case
arbeidsforholdDokument { arbeidsgiverID = ORGNR1 }

// Complex: testing multiple arbeidsforhold interactions
arbeidsforholdDokument {
    arbeidsforhold { arbeidsgiverID = ORGNR1; ansettelsesPeriode(date1, date2) }
    arbeidsforhold { arbeidsgiverID = ORGNR2; ansettelsesPeriode(date3, date4) }
}
```

## Inline DSL Refactoring Guide

When refactoring tests to use the forTest DSL, follow these steps:

### Step 1: Identify Helper Functions to Remove

Look for helper functions that:
- Create domain entities via `Entity().apply { }`
- Return data classes containing multiple entities
- Add items to mutable lists

**Before (anti-pattern):**
```kotlin
private fun lagArbeidsforhold(
    arbDokument: ArbeidsforholdDokument,
    orgnr: String,
    fom: LocalDate,
    tom: LocalDate
): Arbeidsforhold = Arbeidsforhold().apply {
    arbeidsgiverID = orgnr
    ansettelsesPeriode = Periode(fom, tom)
    arbDokument.arbeidsforhold = arbDokument.arbeidsforhold + this
}
```

### Step 2: Replace with DSL Extension

Create or use existing DSL extension that integrates with the factory pattern:

**After (DSL pattern):**
```kotlin
// In SaksopplysningTestFactory.kt
fun SaksopplysningTestFactory.Builder.arbeidsforholdDokument(
    init: ArbeidsforholdDokumentBuilder.() -> Unit
) = apply {
    this.dokument = ArbeidsforholdDokumentBuilder().apply(init).build()
}
```

### Step 3: Update Tests to Inline DSL

Replace complex helper calls with inline DSL blocks:

**Before:**
```kotlin
private fun lagBehandling(): TestOppsett {
    val arbDokument = ArbeidsforholdDokument()
    lagArbeidsforhold(arbDokument, ORGNR2, LocalDate.of(2005, 1, 11), LocalDate.of(2017, 8, 11))

    val behandling = Behandling.forTest {
        saksopplysning { dokument = arbDokument; type = SaksopplysningType.ARBFORH }
    }
    return TestOppsett(behandling, arbDokument)
}

@Test
fun `my test`() {
    val oppsett = lagBehandling()
    // use oppsett.behandling
}
```

**After:**
```kotlin
@Test
fun `my test`() {
    val behandling = Behandling.forTest {
        fagsak { medBruker() }
        saksopplysning {
            type = SaksopplysningType.ARBFORH
            arbeidsforholdDokument {
                arbeidsgiverID = ORGNR2
                ansettelsesPeriode(LocalDate.of(2005, 1, 11), LocalDate.of(2017, 8, 11))
            }
        }
    }
    // use behandling directly
}
```

### Step 4: Simplify Minimal Cases

For tests that don't care about specific setup, create minimal helpers:

```kotlin
private fun lagMinimalBehandling() = Behandling.forTest {
    id = 123L
    fagsak { medBruker() }
    saksopplysning { dokument = ArbeidsforholdDokument(); type = SaksopplysningType.ARBFORH }
    mottatteOpplysninger { mottatteOpplysningerData = soeknadForTest() }
}
```

### Refactoring Checklist

- [ ] Remove `Entity().apply { }` patterns
- [ ] Remove helper functions that return data classes with multiple entities
- [ ] Remove mutable list manipulation in helpers
- [ ] Use DSL extension functions for document types
- [ ] Inline DSL blocks in individual tests
- [ ] Create minimal helpers only for commonly-shared setup
- [ ] Verify all tests still pass

### Benefits of Inline DSL

1. **Readability**: Each test shows exactly what it needs
2. **Maintainability**: No shared mutable state across tests
3. **Discoverability**: IDE autocomplete shows available options
4. **Type safety**: Compile-time errors for invalid combinations

## Default Values

Each TestFactory provides sensible defaults. Check the factory's companion object for constants:

```kotlin
object FagsakTestFactory {
    const val SAKSNUMMER = "MEL-test"
    const val BRUKER_AKTØR_ID = "12345678901"
    const val ORGNR = "123456789"
    val SAKSTYPE = Sakstyper.EU_EOS
    val SAKSTEMA = Sakstemaer.MEDLEMSKAP_LOVVALG
}

object MedlemskapsperiodeTestFactory {
    val FOM = LocalDate.of(2023, 1, 1)
    val TOM = LocalDate.of(2023, 12, 31)
    val INNVILGELSESRESULTAT = InnvilgelsesResultat.INNVILGET
}

object TrygdeavgiftsperiodeTestFactory {
    val PERIODE_FRA = LocalDate.of(2023, 1, 1)
    val PERIODE_TIL = LocalDate.of(2023, 12, 31)
    val TRYGDESATS = BigDecimal("6.8")
}
```

## Anti-Patterns

### Mocking Domain Entities

**Don't mock domain entities when forTest DSL can create real objects.**

```kotlin
// BAD: Mocking domain entity to control behavior
val behandlingsresultatMock = mockk<Behandlingsresultat>()
every { behandlingsresultatMock.behandling } returns behandling
every { behandlingsresultatMock.finnAvgiftspliktigPerioder() } returns emptyList()
every { behandlingsresultatMock.utledAvgiftspliktigperioderFom() } returns null

// GOOD: Create real object that naturally produces the behavior
val behandlingsresultat = Behandlingsresultat.forTest {
    behandling { status = Behandlingsstatus.OPPRETTET }
    // No medlemskapsperioder → finnAvgiftspliktigPerioder() returns empty
    // No innvilget perioder → utledAvgiftspliktigperioderFom() returns null
}
```

**Why real objects are better:**
- Tests verify actual entity behavior, not mock assumptions
- Mocks can create impossible states (e.g., empty periods but non-null fom)
- Real objects catch regressions when entity logic changes
- Cleaner, more readable test code

**When mocks are still appropriate:**
- External services (repositories, clients, APIs)
- Complex dependencies with side effects
- When you need to verify interactions (verify calls)

### Testing Edge Cases

To test edge cases like null dates, update the factory to support nullable fields:

```kotlin
// MedlemskapsperiodeTestFactory supports nullable fom/tom
val behandlingsresultat = Behandlingsresultat.forTest {
    behandling { }
    medlemskapsperiode {
        fom = null  // Tests what happens when fom is null
        tom = LocalDate.now()
    }
}
```

### Mutable Object Creation

Avoid these patterns:
- `Entity().apply { }` patterns outside TestFactory builders
- `lateinit var` for domain entities
- Post-construction mutation

### Post-Construction Mutation

```kotlin
// BAD: Mutating after creation
val oppsett = lagBehandling()
oppsett.soeknad.bosted = Bosted()  // ANTI-PATTERN!

// GOOD: Configure in DSL block
val oppsett = lagBehandling {
    soeknad { bosted = Bosted() }
}
```

### Argument-Based Helper Methods

```kotlin
// BAD: Arguments instead of DSL
private fun lagBehandling(
    id: Long = 1L,
    status: Behandlingsstatus = Behandlingsstatus.OPPRETTET
): Behandling = Behandling.forTest { ... }

// GOOD: DSL init block
private fun lagBehandling(
    init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
): Behandling = Behandling.forTest {
    // defaults
    init()
}
```

### Using Raw Entity Types Instead of Factory Builders

```kotlin
// BAD: Using Soeknad.() -> Unit
private var soeknadBlock: (Soeknad.() -> Unit)? = null

fun soeknad(init: Soeknad.() -> Unit) {
    soeknadBlock = init
}

// Requires post-construction .apply:
val soeknad = soeknadForTest { ... }.apply { soeknadBlock?.invoke(this) }  // Awkward!

// GOOD: Use existing factory builder type
private var soeknadBlock: (SoeknadTestFactory.Builder.() -> Unit)? = null

fun soeknad(init: SoeknadTestFactory.Builder.() -> Unit) {
    soeknadBlock = init
}

// Apply INSIDE the DSL block:
val soeknad = soeknadForTest {
    // defaults...
    soeknadBlock?.invoke(this)  // Clean!
}
```

### Post-Construction .apply Patterns

```kotlin
// BAD: .apply after construction
val soeknad = soeknadForTest {
    // defaults
}.apply { builder.soeknadBlock?.invoke(this) }  // Awkward post-construction!

// GOOD: Apply inside the DSL block
val soeknad = soeknadForTest {
    // defaults
    builder.soeknadBlock?.invoke(this)  // Clean - all in one block
}
```

### Inconsistent Entity Creation

```kotlin
// BAD: Direct Entity().apply when forTest DSL exists
val soeknad = Soeknad().apply {
    bosted.oppgittAdresse.landkode = Landkoder.NO.kode
    bosted.oppgittAdresse.gatenavn = "HjemmeGata"
}

// GOOD: Use forTest DSL consistently
val soeknad = soeknadForTest {
    bostedAdresse(landkode = Landkoder.NO.kode, gatenavn = "HjemmeGata")
}
```

### Helper Functions That Mutate Parameters

```kotlin
// BAD: Helper that modifies passed document
private fun lagArbeidsforhold(
    arbDokument: ArbeidsforholdDokument,
    orgnr: String,
    fom: LocalDate,
    tom: LocalDate
): Arbeidsforhold = Arbeidsforhold().apply {
    arbeidsgiverID = orgnr
    ansettelsesPeriode = Periode(fom, tom)
    arbDokument.arbeidsforhold = arbDokument.arbeidsforhold + this  // Side effect!
}

// GOOD: Use DSL extension that builds document internally
saksopplysning {
    type = SaksopplysningType.ARBFORH
    arbeidsforholdDokument {
        arbeidsgiverID = orgnr
        ansettelsesPeriode(fom, tom)
    }
}
```

### Complex TestOppsett Data Classes

```kotlin
// BAD: Returning multiple objects in data class
private data class TestOppsett(
    val behandling: Behandling,
    val arbDokument: ArbeidsforholdDokument,
    val soeknad: Soeknad
)

private fun lagBehandling(): TestOppsett {
    val arbDokument = ArbeidsforholdDokument()
    val soeknad = Soeknad()
    // ... complex setup
    return TestOppsett(behandling, arbDokument, soeknad)
}

// GOOD: Inline DSL in each test, or minimal helper returning single entity
val behandling = Behandling.forTest {
    saksopplysning {
        type = SaksopplysningType.ARBFORH
        arbeidsforholdDokument { arbeidsgiverID = "123" }
    }
    mottatteOpplysninger { soeknad { bostedAdresse(landkode = "NO") } }
}
```

### Hard-to-Read Parameter Lists

```kotlin
// BAD: Long parameter lists
private fun lagBehandling(
    foretakUtlandOrgnr: String = ORGNR1,
    selvstendigForetakOrgnr: List<String> = emptyList(),
    ekstraArbeidsgivere: List<String> = emptyList(),
    bostedLandkode: String = "NO",
    bostedGatenavn: String = "TestGata"
): Behandling = ...

// GOOD: Inline DSL with clear structure
val behandling = Behandling.forTest {
    mottatteOpplysninger {
        soeknad {
            foretakUtland(ORGNR1)
            selvstendigForetak(ORGNR2)
            ekstraArbeidsgiver(ORGNR3)
            bostedAdresse(landkode = "NO", gatenavn = "TestGata")
        }
    }
}
```
