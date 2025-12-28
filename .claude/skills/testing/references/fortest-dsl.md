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
| `Medlemskapsperiode` | domain | `MedlemskapsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Lovvalgsperiode` | domain | `LovvalgsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Trygdeavgiftsperiode` | domain | `TrygdeavgiftsperiodeTestFactory` | `domain/src/test/kotlin/.../domain/avgift/` |
| `MottatteOpplysninger` | domain | `MottatteOpplysningerTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `Soeknad` | domain | `SoeknadTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `AnmodningEllerAttest` | domain | `AnmodningEllerAttestTestFactory` | `domain/src/test/kotlin/.../mottatteopplysninger/` |
| `Saksopplysning` | domain | `SaksopplysningTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `PersonDokument` | domain | `PersonDokumentTestFactory` | `domain/src/test/kotlin/.../dokument/` |
| `OrganisasjonDokument` | domain | `OrganisasjonDokumentTestFactory` | `domain/src/test/kotlin/.../domain/` |
| `Årsavregning` | domain | `ÅrsavregningTestFactory` | `domain/src/test/kotlin/.../domain/` |
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

## Complex Test Setup Patterns

When tests need access to nested objects for edge case modifications, use a data class:

```kotlin
private data class TestOppsett(
    val behandling: Behandling,
    val arbDokument: ArbeidsforholdDokument,
    val soeknad: Soeknad
)

private fun lagBehandling(
    foretakUtlandOrgnr: String = ORGNR1,
    selvstendigForetakOrgnr: List<String> = emptyList()
): TestOppsett {
    val arbDokument = ArbeidsforholdDokument()

    val soeknad = Soeknad().apply {
        bosted.oppgittAdresse.landkode = Landkoder.NO.kode
        // ... more setup
    }

    val behandling = Behandling.forTest {
        fagsak { medBruker() }
        saksopplysning {
            dokument = arbDokument
            type = SaksopplysningType.ARBFORH
        }
        mottatteOpplysninger {
            mottatteOpplysningerData = soeknad
        }
    }

    return TestOppsett(behandling, arbDokument, soeknad)
}

// Usage in test
@Test
fun `test edge case with empty bosted`() {
    val oppsett = lagBehandling()
    oppsett.soeknad.bosted = Bosted()  // Modify for edge case
    // ... test logic
}
```

This pattern:
- Keeps most data immutable through DSL
- Allows controlled modification for edge case tests
- Makes test intent clear

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

See the main testing skill for patterns to avoid:
- `Entity().apply { }` patterns
- `lateinit var` for domain entities
- Post-construction mutation
