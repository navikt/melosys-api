# BUC Types Reference

## Overview

BUC (Business Use Case) defines the workflow/case type in EESSI. Each BUC has a specific purpose and set of allowed SEDs.

## LA_BUC (Applicable Legislation)

### LA_BUC_01 - Exception Agreements (Art. 16)

**Purpose**: Request exception from normal applicable legislation rules.

**Typical Flow**:
1. Norway sends A001 (request for exception)
2. Other country sends A011 (acceptance) or A002 (refusal)

**Initiating SED**: A001
**Response SEDs**: A011, A002

**Bestemmelser mapping**:
```kotlin
Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1 -> LA_BUC_01
Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2 -> LA_BUC_01
```

### LA_BUC_02 - Work in Multiple Countries (Art. 13)

**Purpose**: Determine applicable legislation for persons working in multiple member states.

**Typical Flow**:
1. Employer or person applies
2. Norway determines legislation
3. Sends A003 to involved countries
4. Countries respond with A004 (objection) or no response (accepted)
5. Norway sends A012 (confirmation)

**Initiating SED**: A003
**Response SEDs**: A004, A012

**Bestemmelser mapping**:
```kotlin
Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A -> LA_BUC_02
Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1 -> LA_BUC_02
Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A -> LA_BUC_02
// ... etc
```

### LA_BUC_04 - Posted Workers (Art. 12)

**Purpose**: Notification that a worker is posted to another country but remains covered by home country.

**Initiating SED**: A003
**Response SEDs**: A004, A012

**Bestemmelser mapping**:
```kotlin
Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1 -> LA_BUC_04
Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2 -> LA_BUC_04
```

### LA_BUC_05 - General Determination (Art. 11)

**Purpose**: General notification of applicable legislation.

**Initiating SED**: A005 (notification)
**Response SEDs**: Various

**Bestemmelser mapping**:
```kotlin
Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1 -> LA_BUC_05
Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A -> LA_BUC_05
Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B -> LA_BUC_05
// ... etc
```

### LA_BUC_06 - Family Member Membership

**Purpose**: Determination of legislation for family members.

**Initiating SED**: A003

## H_BUC (Health Benefits)

### H_BUC_01 - Sickness Benefits in Kind

For coordinating health benefit entitlements.

### H_BUC_02a/b/c - Various Health Scenarios

Different sub-types for specific health coordination needs.

## BucType Enum

```kotlin
enum class BucType {
    LA_BUC_01,  // Exception agreements
    LA_BUC_02,  // Work in multiple countries
    LA_BUC_03,  // Notification
    LA_BUC_04,  // Posted workers
    LA_BUC_05,  // General determination
    LA_BUC_06,  // Family members

    H_BUC_01, H_BUC_02a, H_BUC_02b, H_BUC_02c,
    H_BUC_03a, H_BUC_03b, H_BUC_04, H_BUC_05,
    H_BUC_06, H_BUC_07, H_BUC_08, H_BUC_09, H_BUC_10,

    UB_BUC_01;  // Unemployment benefits
}
```

## BUC Lifecycle States

| State | Description |
|-------|-------------|
| `open` | BUC is active, SEDs can be sent |
| `closed` | BUC is closed, no more SEDs |
| `received` | BUC received from another country |
| `sent` | Initial SED has been sent |

## Determining BUC from Bestemmelse

```kotlin
// BucType.fraBestemmelse() maps lovvalgsbestemmelse to correct BUC
val bucType = BucType.fraBestemmelse(behandlingsresultat.lovvalgsbestemmelse)
```

## BucInformasjon

```kotlin
data class BucInformasjon(
    val id: String?,           // RINA BUC ID
    val erÅpen: Boolean,       // Still open for SEDs?
    val bucType: String?,      // LA_BUC_02, etc.
    val opprettetDato: LocalDate,
    val mottakerinstitusjoner: Set<String>?,
    val seder: List<SedInformasjon>  // SEDs in this BUC
)
```

## Checking BUC Status

```kotlin
// Check if BUC is open
val erÅpen = eessiService.erBucAapen(arkivsakID)

// Get all BUCs for a case
val bucer = eessiService.hentTilknyttedeBucer(arkivsakID, listOf())
```
