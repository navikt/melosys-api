# SED Types Reference

## Overview

SED (Structured Electronic Document) is the standardized document format used in EESSI for exchanging social security information.

## A-series (Applicable Legislation)

### A001 - Request for Exception Agreement

**BUC**: LA_BUC_01
**Direction**: Outgoing (Norway requests exception)
**Purpose**: Request another country to agree to an exception from normal applicable legislation rules under Art. 16.

**Content**:
- Person identification
- Requested exception period
- Justification for exception
- Proposed applicable legislation

### A002 - Refusal of Exception Request

**BUC**: LA_BUC_01
**Direction**: Incoming (response to A001)
**Purpose**: Other country refuses the exception request.

**Content**:
- Reference to original A001
- Reason for refusal

### A003 - Determination of Applicable Legislation

**BUC**: LA_BUC_02, LA_BUC_04
**Direction**: Both (Norway can send or receive)
**Purpose**: Request provisional determination of applicable legislation for persons working in multiple countries.

**Content**:
- Person identification
- Employment details (employers, countries, work percentages)
- Proposed applicable legislation
- Period

### A004 - Objection to Determination

**BUC**: LA_BUC_02, LA_BUC_04
**Direction**: Both
**Purpose**: Object to a provisional determination made in A003.

**Content**:
- Reference to original A003
- Grounds for objection
- Alternative proposal

### A005 - Notification of Applicable Legislation

**BUC**: LA_BUC_03, LA_BUC_05
**Direction**: Outgoing
**Purpose**: Notify other countries about determined applicable legislation.

### A011 - Acceptance of Exception Request

**BUC**: LA_BUC_01
**Direction**: Incoming (response to A001)
**Purpose**: Other country accepts the exception request.

### A012 - Confirmation of Applicable Legislation

**BUC**: LA_BUC_02
**Direction**: Outgoing
**Purpose**: Final confirmation after no objections received (or objections resolved).

## X-series (Administrative)

### X001 - Request for Information

**Purpose**: Request additional information about a case.

### X008 - Invalidation

**Purpose**: Invalidate a previously sent SED (e.g., when re-assessing a case).

**Content**:
```kotlin
data class InvalideringSedDto(
    var sedTypeSomSkalInvalideres: String?,  // "A012"
    var utstedelsedato: String?              // Original SED date
)
```

### X009 - Reminder (Purring)

**Purpose**: Send reminder when no response received.

```kotlin
fun erPurring(): Boolean = this == X009
```

## SedType Enum

```kotlin
enum class SedType {
    // X-series (Administrative)
    X001, X002, X003, X004, X005, X006, X007, X008, X009,
    X010, X011, X012, X013, X050, X100,

    // A-series (Applicable Legislation)
    A001, A002, A003, A004, A005, A006, A007, A008, A009,
    A010, A011, A012,

    // H-series (Health)
    H001, H002, H003, H004, H005, H006, H010, H011, H012,
    H020, H061, H065, H066, H070, H120, H121, H130,

    // S-series (Sickness benefits)
    S040, S041;

    fun erPurring(): Boolean = this == X009
}
```

## SED Data Structure

### SedDataDto

```kotlin
data class SedDataDto(
    var mottakerIder: List<String>?,         // Recipient institution IDs
    var gsakSaksnummer: Long?,               // NAV case number
    var ytterligereInformasjon: String?,     // Free text
    var invalideringSedDto: InvalideringSedDto?,  // For X008
    var utpekingAvvis: UtpekingAvvisDto?,    // For A004
    // ... person data, periods, employers, etc.
)
```

### SedGrunnlagDto

Content extracted from received SED:
```kotlin
data class SedGrunnlagDto(
    val arbeidsland: List<Arbeidsland>?,
    val lovvalgsperiode: Lovvalgsperiode?,
    val bestemmelse: Bestemmelse?,
    // ...
)
```

## SED to Behandlingstema Mapping

```kotlin
// SedTypeTilBehandlingstemaMapper
fun finnBehandlingstemaForSedType(sedType: String, lovvalgsland: String): Optional<Behandlingstema>
```

Maps incoming SED to correct behandlingstema for creating behandling.

## Period Types in SED

```kotlin
enum class PeriodeType {
    LOVVALGSPERIODE,      // Main applicable legislation period
    ANMODNINGSPERIODE,    // Exception request period (Art. 16)
    UTPEKINGSPERIODE,     // Designation period (A003 response)
    INGEN                 // Administrative SEDs (X-series)
}
```

## SED Routing

Incoming SEDs are routed to specific handlers:

| SED Type | Router |
|----------|--------|
| A001 | AnmodningOmUnntakSedRuter |
| A003 | ArbeidFlereLandSedRuter |
| A011, A002 | SvarAnmodningUnntakSedRuter |
| Others | DefaultSedRuter |
