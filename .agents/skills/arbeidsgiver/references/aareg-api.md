# AAREG API Reference

## Overview

AAREG (Arbeidsgiver- og arbeidstakerregisteret) is the Norwegian Employment Register
containing all employment relationships reported through the a-ordning system.
Melosys accesses AAREG via REST API.

## REST Endpoint

```
GET /
Header: Nav-Personident: {fnr}
Query params: regelverk, arbeidsforholdType, ansettelsesperiodeFom, ansettelsesperiodeTom
```

Returns list of employment relationships for the given person.

## ArbeidsforholdConsumer

```kotlin
@Retryable
open class ArbeidsforholdConsumer(private val webClient: WebClient) {
    open fun finnArbeidsforholdPrArbeidstaker(
        fnr: String,
        arbeidsforholdQuery: ArbeidsforholdQuery
    ): ArbeidsforholdResponse {
        return ArbeidsforholdResponse(hentArbeidsforhold(fnr, arbeidsforholdQuery))
    }

    private fun hentArbeidsforhold(
        fnr: String,
        arbeidsforholdQuery: ArbeidsforholdQuery
    ): List<ArbeidsforholdResponse.Arbeidsforhold> {
        return webClient.get()
            .uri("") { uriBuilder ->
                uriBuilder
                    .queryParam("regelverk", arbeidsforholdQuery.regelverk)
                    .queryParamIfPresent("arbeidsforholdType", ...)
                    .queryParamIfPresent("ansettelsesperiodeFom", ...)
                    .queryParamIfPresent("ansettelsesperiodeTom", ...)
                    .build()
            }
            .header("Nav-Personident", fnr)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<List<ArbeidsforholdResponse.Arbeidsforhold>>()
            .block() ?: throw TekniskException("ArbeidsforholdResponse er null")
    }
}
```

## Query Parameters

```kotlin
data class ArbeidsforholdQuery(
    val regelverk: Regelverk = Regelverk.ALLE,
    val arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ALLE,
    val ansettelsesperiodeFom: LocalDate? = null,
    val ansettelsesperiodeTom: LocalDate? = null
)
```

### Regelverk

| Value | Description |
|-------|-------------|
| `ALLE` | All data sources |
| `A_ORDNINGEN` | Current reporting system (2015+) |
| `FOER_A_ORDNINGEN` | Pre-2015 historical data |

### ArbeidsforholdType

| Value | Query Param | Description |
|-------|-------------|-------------|
| `ORDINAERT_ARBEIDSFORHOLD` | ordinaertArbeidsforhold | Regular employment |
| `MARITIMT_ARBEIDSFORHOLD` | maritimtArbeidsforhold | Maritime employment |
| `FRILANSER_OPPDRAGSTAKER_HONORARPE_RSONER_MM` | frilanserOppdragstakerHonorarPersonerMm | Freelance/contract |
| `FORENKLET_OPPGJOERSORDNING` | forenkletOppgjoersordning | Simplified settlement |
| `ALLE` | (null) | All types |

## Response DTOs

### Arbeidsforhold

```kotlin
data class Arbeidsforhold(
    val arbeidsforholdId: String?,      // Employer's internal ID
    val navArbeidsforholdId: Int,       // NAV's unique ID
    val ansettelsesperiode: Ansettelsesperiode,
    val type: String?,                   // Employment type code

    // Parties
    val arbeidstaker: Arbeidstaker,
    val arbeidsgiver: Arbeidsgiver,
    val opplysningspliktig: Opplysningspliktig,

    // Contract details
    val arbeidsavtaler: List<Arbeidsavtale>?,

    // Special circumstances
    val permisjonPermitteringer: List<PermisjonPermittering>?,
    val utenlandsopphold: List<Utenlandsopphold>?,

    // Metadata
    val innrapportertEtterAOrdningen: Boolean?,
    val registrert: String?,             // ISO datetime
    val sistBekreftet: String?,          // ISO datetime
    val antallTimerForTimeloennet: List<AntallTimerForTimeloennet>?
)
```

### Arbeidsavtale (Work Contract)

```kotlin
data class Arbeidsavtale(
    val type: String?,                   // Forenklet, Frilanser, Maritim, Ordinaer
    val arbeidstidsordning: String?,     // Work time arrangement code
    val yrke: String?,                   // Occupation code (STYRK)
    val ansettelsesform: String?,        // Employment form code
    val stillingsprosent: BigDecimal?,   // Position percentage (0-100)
    val beregnetAntallTimerPrUke: BigDecimal?,
    val antallTimerPrUke: BigDecimal?,
    val gyldighetsperiode: Periode,
    val sistStillingsendring: LocalDate?,
    val sistLoennsendring: LocalDate?
)
```

### Arbeidsgiver (Employer)

```kotlin
data class Arbeidsgiver(
    val type: String,              // "Organisasjon" or "Person"
    val organisasjonsnummer: String?
)
```

### Opplysningspliktig (Reporting Entity)

```kotlin
data class Opplysningspliktig(
    val type: String,              // "Organisasjon" or "Person"
    val organisasjonsnummer: String?
)
```

### PermisjonPermittering (Leave/Layoff)

```kotlin
data class PermisjonPermittering(
    val periode: Periode,
    val permisjonPermitteringId: String?,
    val prosent: BigDecimal?,      // Leave percentage
    val type: String?,             // Type code
    val varslingskode: String?
)
```

### Utenlandsopphold (Foreign Stay)

```kotlin
data class Utenlandsopphold(
    val landkode: String?,         // ISO country code
    val periode: Periode,
    val rapporteringsperiode: String  // YYYY-MM
)
```

### Periode

```kotlin
data class Periode(
    val fom: LocalDate?,
    val tom: LocalDate?
)
```

## Domain Model Mapping

### ArbeidsforholdKonverter

Converts AAREG response to domain model:

```kotlin
class ArbeidsforholdKonverter(
    private val arbeidsforholdResponse: ArbeidsforholdResponse,
    private val kodeverkService: KodeverkService
) {
    fun createSaksopplysning(): Saksopplysning {
        return Saksopplysning().apply {
            dokument = ArbeidsforholdDokument(
                arbeidsforholdResponse.arbeidsforhold.map { src ->
                    Arbeidsforhold().apply {
                        arbeidsforholdID = src.arbeidsforholdId
                        arbeidsforholdIDnav = src.navArbeidsforholdId.toLong()
                        ansettelsesPeriode = getPeriode(src.periode)
                        arbeidsforholdstype = src.type
                        arbeidsavtaler = getArbeidsAvtaler(src.arbeidsavtaler)
                        permisjonOgPermittering = getPermisjonPermitteringer(src.permisjonPermitteringer)
                        utenlandsopphold = getUtenlandsopphold(src.utenlandsopphold)
                        arbeidsgivertype = Aktoertype.valueOf(src.arbeidsgiver.type.uppercase())
                        arbeidsgiverID = src.arbeidsgiver.organisasjonsnummer
                        arbeidstakerID = src.arbeidstaker.offentligIdent
                        opplysningspliktigtype = Aktoertype.valueOf(src.opplysningspliktigtype)
                        opplysningspliktigID = src.opplysningspliktig.organisasjonsnummer
                    }
                }
            )
        }
    }
}
```

## Domain Models

### Arbeidsforhold

```kotlin
class Arbeidsforhold : HarPeriode {
    var arbeidsforholdID: String?        // Employer's ID
    var arbeidsforholdIDnav: Long        // NAV's ID
    var ansettelsesPeriode: Periode?
    var arbeidsforholdstype: String?
    var arbeidsavtaler: List<Arbeidsavtale>
    var permisjonOgPermittering: List<PermisjonOgPermittering>
    var utenlandsopphold: List<Utenlandsopphold>

    // Employer info
    var arbeidsgivertype: Aktoertype?    // ORGANISASJON or PERSON
    var arbeidsgiverID: String?          // Org number (if ORGANISASJON)
    var arbeidstakerID: String?          // Person's fnr

    // Reporting entity
    var opplysningspliktigtype: Aktoertype?
    var opplysningspliktigID: String?

    var arbeidsforholdInnrapportertEtterAOrdningen: Boolean?

    fun hentOrgnumre(): List<String>     // Returns both arbeidsgiver and opplysningspliktig orgnr
}
```

### ArbeidsforholdDokument

```kotlin
class ArbeidsforholdDokument(
    val arbeidsforhold: List<Arbeidsforhold>
) : SaksopplysningDokument {
    fun hentOrgnumre(): Set<String>      // Unique org numbers from all relationships
}
```

### Aktoertype

```kotlin
enum class Aktoertype {
    ORGANISASJON,
    PERSON
}
```

## Kodeverk (Code Sets)

Employment data uses several code sets from kodeverk-api:

| Kodeverk | Description | Example |
|----------|-------------|---------|
| `Arbeidsforholdstyper` | Employment type | ordinaertArbeidsforhold |
| `Yrker` | Occupations (STYRK) | 2512 - Programvareutvikler |
| `Arbeidstidsordninger` | Work time arrangements | ikkeSkift |
| `PermisjonsOgPermitteringsBeskrivelse` | Leave/layoff types | permisjonMedForeldrepenger |
| `AnsettelsesformAareg` | Employment forms | fast, midlertidig |

## ArbeidsforholdService

Service layer wrapper:

```kotlin
@Service
class ArbeidsforholdService(
    private val arbeidsforholdConsumer: ArbeidsforholdConsumer,
    private val kodeverkService: KodeverkService
) {
    fun finnArbeidsforholdPrArbeidstaker(
        ident: String,
        fom: LocalDate?,
        tom: LocalDate?
    ): Saksopplysning {
        val query = ArbeidsforholdQuery(
            regelverk = Regelverk.A_ORDNINGEN,
            arbeidsforholdType = ArbeidsforholdType.ALLE,
            ansettelsesperiodeFom = fom,
            ansettelsesperiodeTom = tom
        )
        val response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(ident, query)
        return ArbeidsforholdKonverter(response, kodeverkService)
            .createSaksopplysning()
            .apply {
                leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.AAREG, response.tilSaksopplysning())
                type = SaksopplysningType.ARBFORH
                versjon = "REST 1.0"
            }
    }
}
```

## Error Handling

| Exception | Cause |
|-----------|-------|
| `TekniskException("ArbeidsforholdResponse er null")` | No response from AAREG |
| Network errors | AAREG service unavailable |

The `@Retryable` annotation on the consumer provides automatic retry for transient failures.
