---
name: arbeidsgiver
description: |
  Expert knowledge of Employer/Employment integration (EREG/AAREG) in melosys-api.
  Use when: (1) Working with organization data from EREG (Enhetsregisteret),
  (2) Working with employment data from AAREG (Arbeidsforhold),
  (3) Understanding employer/organization lookup,
  (4) Debugging employer or employment issues,
  (5) Understanding the mapping between register data and domain models.
---

# Arbeidsgiver (Employer/Employment) System

Melosys retrieves employer and employment data from two registers:
- **EREG** (Enhetsregisteret): Organization data - names, addresses, sector codes
- **AAREG** (Arbeidsgiver- og arbeidstakerregisteret): Employment relationships - work contracts, periods

This data is used for case processing, determining applicable legislation, and letter generation.

## Quick Reference

### Module Structure

```
integrasjon/ereg/
├── EregFasade.kt                 # Interface for EREG operations
├── EregRestService.kt            # Implementation
├── EregDtoTilSaksopplysningKonverter.kt  # DTO → domain mapper
└── organisasjon/
    ├── OrganisasjonRestConsumer.kt       # REST client
    ├── OrganisasjonRestConsumerConfig.kt # WebClient config
    └── OrganisasjonResponse.kt           # Response DTOs

integrasjon/aareg/arbeidsforhold/
├── ArbeidsforholdConsumer.kt     # REST client for AAREG
├── ArbeidsforholdConsumerConfig.kt
├── ArbeidsforholdQuery.kt        # Query parameters
└── ArbeidsforholdResponse.kt     # Response DTOs

service/aareg/
├── ArbeidsforholdService.kt      # Service layer for employment
└── ArbeidsforholdKonverter.kt    # Response → domain mapper

service/registeropplysninger/
├── RegisteropplysningerService.java  # Coordinates all register lookups
└── OrganisasjonOppslagService.java   # Organization lookup helper

domain/dokument/arbeidsforhold/
├── Arbeidsforhold.kt             # Employment relationship
├── ArbeidsforholdDokument.kt     # Collection of employments
├── Arbeidsavtale.kt              # Work contract
├── PermisjonOgPermittering.kt    # Leave/layoff
└── Utenlandsopphold.kt           # Foreign stays

domain/dokument/organisasjon/
├── OrganisasjonDokument.kt       # Organization entity
├── OrganisasjonsDetaljer.kt      # Details (addresses, contacts)
└── Organisasjonsnavn.kt          # Name history
```

### Key Services

| Service | Purpose |
|---------|---------|
| `EregFasade` | Lookup organization by orgnummer |
| `EregRestService` | EREG REST implementation |
| `ArbeidsforholdService` | Fetch employment history for person |
| `ArbeidsforholdConsumer` | REST client to AAREG |
| `OrganisasjonOppslagService` | Organization lookup helper |
| `RegisteropplysningerService` | Coordinates all register lookups |

## EREG (Organization Data)

### Operations

```kotlin
// Lookup organization (throws if not found)
val saksopplysning = eregFasade.hentOrganisasjon(orgnummer)
val orgDokument = saksopplysning.dokument as OrganisasjonDokument

// Optional lookup
val optSaksopplysning = eregFasade.finnOrganisasjon(orgnummer)

// Get name only
val navn = eregFasade.hentOrganisasjonNavn(orgnummer)

// Lookup multiple via helper service
val orgnumre = setOf("123456789", "987654321")
val organisasjoner = organisasjonOppslagService.hentOrganisasjoner(orgnumre)
```

### Organization Types

| Type | Description |
|------|-------------|
| `JuridiskEnhet` | Legal entity (AS, ANS, etc.) |
| `Virksomhet` | Business unit (underenhet) |
| `Organisasjonsledd` | Organizational unit |

### Organization Response

```kotlin
class OrganisasjonResponse.Organisasjon(
    val organisasjonsnummer: String,      // 9-digit org number
    val navn: Navn?,                       // Current name
    val organisasjonDetaljer: OrganisasjonDetaljer?
)

data class OrganisasjonDetaljer(
    val registreringsdato: LocalDateTime?,
    val opphoersdato: LocalDate?,          // Closure date
    val forretningsadresser: List<Adresse>?,
    val postadresser: List<Adresse>?,
    val telefonnummer: List<Telefonnummer>?,
    val epostadresser: List<Epostadresse>?,
    val naeringer: List<Naering>?,         // Industry codes
    val enhetstyper: List<Enhetstype>?,
    val ansatte: List<Ansatte>?            // Employee count
)
```

### Organization Domain Model

```kotlin
class OrganisasjonDokument(
    val orgnummer: String,
    val navn: String,
    val enhetstype: String?,       // AS, ENK, etc.
    val sektorkode: String,        // Sector code
    val organisasjonDetaljer: OrganisasjonsDetaljer
) {
    fun harRegistrertPostadresse(): Boolean
    fun harRegistrertForretningsadresse(): Boolean
    fun hentTilgjengeligAdresse(): StrukturertAdresse?
}
```

## AAREG (Employment Data)

### Operations

```kotlin
// Fetch employment history for a person
val saksopplysning = arbeidsforholdService.finnArbeidsforholdPrArbeidstaker(
    ident = fnr,
    fom = LocalDate.of(2020, 1, 1),
    tom = LocalDate.of(2024, 12, 31)
)
val arbeidsforholdDokument = saksopplysning.dokument as ArbeidsforholdDokument
```

### Query Parameters

```kotlin
data class ArbeidsforholdQuery(
    val regelverk: Regelverk = Regelverk.A_ORDNINGEN,
    val arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ALLE,
    val ansettelsesperiodeFom: LocalDate? = null,
    val ansettelsesperiodeTom: LocalDate? = null
)

enum class Regelverk {
    ALLE,
    A_ORDNINGEN,       // Current reporting system
    FOER_A_ORDNINGEN   // Pre-2015 data
}

enum class ArbeidsforholdType {
    ORDINAERT_ARBEIDSFORHOLD,
    MARITIMT_ARBEIDSFORHOLD,
    FRILANSER_OPPDRAGSTAKER_HONORARPE_RSONER_MM,
    FORENKLET_OPPGJOERSORDNING,
    ALLE
}
```

### Employment Response

```kotlin
data class Arbeidsforhold(
    val arbeidsforholdId: String?,         // Employer's ID
    val navArbeidsforholdId: Int,          // NAV's ID
    val ansettelsesperiode: Ansettelsesperiode,
    val type: String?,                      // Employment type
    val arbeidstaker: Arbeidstaker,
    val arbeidsavtaler: List<Arbeidsavtale>?,
    val permisjonPermitteringer: List<PermisjonPermittering>?,
    val utenlandsopphold: List<Utenlandsopphold>?,
    val arbeidsgiver: Arbeidsgiver,         // Employer info
    val opplysningspliktig: Opplysningspliktig,
    val innrapportertEtterAOrdningen: Boolean?
)

data class Arbeidsavtale(
    val type: String?,              // Ordinaer, Frilanser, etc.
    val yrke: String?,              // Occupation code
    val stillingsprosent: BigDecimal?,
    val gyldighetsperiode: Periode,
    val beregnetAntallTimerPrUke: BigDecimal?
)
```

### Employment Domain Model

```kotlin
class Arbeidsforhold {
    var arbeidsforholdID: String?
    var arbeidsforholdIDnav: Long
    var ansettelsesPeriode: Periode?
    var arbeidsforholdstype: String?
    var arbeidsavtaler: List<Arbeidsavtale>
    var permisjonOgPermittering: List<PermisjonOgPermittering>
    var utenlandsopphold: List<Utenlandsopphold>
    var arbeidsgivertype: Aktoertype?  // ORGANISASJON or PERSON
    var arbeidsgiverID: String?         // Orgnummer
    var opplysningspliktigtype: Aktoertype?
    var opplysningspliktigID: String?

    fun hentOrgnumre(): List<String>    // Get all org numbers
}

enum class Aktoertype {
    ORGANISASJON,
    PERSON
}
```

## Register Data Flow

### Fetching Register Data

```
RegisteropplysningerService.hentOgLagreOpplysninger()
    │
    ├── ARBFORH → ArbeidsforholdService → ArbeidsforholdConsumer
    │                                          ↓
    │                                     ArbeidsforholdResponse
    │                                          ↓
    │                                     ArbeidsforholdKonverter
    │                                          ↓
    │                                     ArbeidsforholdDokument
    │
    └── ORG → EregFasade → OrganisasjonRestConsumer
                               ↓
                          OrganisasjonResponse
                               ↓
                          EregDtoTilSaksopplysningKonverter
                               ↓
                          OrganisasjonDokument
```

### Usage in Case Processing

1. **RegisteropplysningerService** fetches employment data (ARBFORH)
2. Extracts organization numbers from employment records
3. Fetches organization details (ORG) for each employer
4. All data stored as `Saksopplysning` on `Behandling`

## Saksopplysning Types

| Type | Source | Description |
|------|--------|-------------|
| `ARBFORH` | AAREG | Employment history |
| `ORG` | EREG | Organization details |
| `INNTK` | Inntekt | Income data |
| `MEDL` | MEDL | Membership periods |

## Common Issues

### Issue: Organization Not Found

**Symptom**: `IkkeFunnetException` when looking up orgnummer

**Causes**:
- Invalid organization number (must be 9 digits)
- Organization doesn't exist or is deleted
- Using fnr instead of orgnr (11 digits rejected)

**Check**:
```kotlin
// Validate orgnr length
if (orgnr.length == 11) {
    log.warn("Looks like fnr, not orgnr: $orgnr")
}
```

### Issue: Empty Employment History

**Symptom**: No arbeidsforhold returned

**Causes**:
- Person has no registered employment
- Date range doesn't overlap any employment
- Using wrong Regelverk (A_ORDNINGEN vs FOER_A_ORDNINGEN)

**Check**:
```kotlin
val query = ArbeidsforholdQuery(
    regelverk = Regelverk.ALLE,  // Try all data sources
    arbeidsforholdType = ArbeidsforholdType.ALLE,
    ansettelsesperiodeFom = fom.minusYears(5),  // Expand range
    ansettelsesperiodeTom = tom
)
```

### Issue: Missing Organization Address

**Symptom**: `harRegistrertAdresse()` returns false

**Cause**: Organization has no registered address in EREG

**Check**:
```kotlin
val org = organisasjonOppslagService.hentOrganisasjon(orgnr)
log.info("Postadresse: ${org.harRegistrertPostadresse()}")
log.info("Forretningsadresse: ${org.harRegistrertForretningsadresse()}")
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| EREG Facade | `integrasjon/.../ereg/EregFasade.kt` |
| EREG Service | `integrasjon/.../ereg/EregRestService.kt` |
| EREG Consumer | `integrasjon/.../ereg/organisasjon/OrganisasjonRestConsumer.kt` |
| EREG DTOs | `integrasjon/.../ereg/organisasjon/OrganisasjonResponse.kt` |
| AAREG Service | `service/.../aareg/ArbeidsforholdService.kt` |
| AAREG Consumer | `integrasjon/.../aareg/arbeidsforhold/ArbeidsforholdConsumer.kt` |
| AAREG DTOs | `integrasjon/.../aareg/arbeidsforhold/ArbeidsforholdResponse.kt` |
| Domain Models | `domain/.../dokument/arbeidsforhold/` |
| Org Domain | `domain/.../dokument/organisasjon/` |
| Register Service | `service/.../registeropplysninger/RegisteropplysningerService.java` |

## Detailed Documentation

- **[EREG API](references/ereg-api.md)**: Organization lookup reference
- **[AAREG API](references/aareg-api.md)**: Employment data reference
- **[Debugging](references/debugging.md)**: Troubleshooting guide
