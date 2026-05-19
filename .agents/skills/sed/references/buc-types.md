# BUC Types Reference

BUC (Business Use Case) groups related SEDs into a workflow/case.

## LA-Series (Applicable Legislation)

| BUC | Name | Purpose | Key SEDs |
|-----|------|---------|----------|
| `LA_BUC_01` | Applicable legislation determination | Normal law choice process | A001, A002, A003, A004, A005 |
| `LA_BUC_02` | Posted worker notification | Notify about posting | A003, A007 |
| `LA_BUC_03` | Multi-state worker | Work in multiple countries | A003 |
| `LA_BUC_04` | Exception agreement (Art. 16) | Request exception from rules | A001, A011, A004 |
| `LA_BUC_05` | Recovery | Recover overpayments | X003, X004 |
| `LA_BUC_06` | Certificate correction | Correct previous A003 | A003, X008 |

### BUC Selection Logic

```kotlin
// BucType.kt - maps lovvalgsbestemmelse to BUC
fun fraBestemmelse(bestemmelse: LovvalgBestemmelse): BucType {
    return when (bestemmelse) {
        ART_12_1, ART_12_2 -> LA_BUC_02  // Posted workers
        ART_13_1, ART_13_2, ART_13_3 -> LA_BUC_03  // Multi-state
        ART_16_1 -> LA_BUC_04  // Exception agreement
        else -> LA_BUC_01  // Default
    }
}
```

## H-Series (Healthcare)

| BUC | Name | Purpose | Key SEDs |
|-----|------|---------|----------|
| `H_BUC_01` | Healthcare entitlement | S1 registration | H001, H002, H003 |
| `H_BUC_02` | Healthcare request | Request coverage info | H001, H002 |
| `H_BUC_03` | Temporary stay | EHIC-related | H004 |
| `H_BUC_04` | Planned treatment | S2 process | H003 |
| `H_BUC_05` | Insurance periods | Period verification | H005, H006, H020, H021 |
| `H_BUC_06` | Cost claims | Reimbursement | H120, H121 |
| `H_BUC_07` | Direct payment | Actual cost settlement | H061, H062 |
| `H_BUC_10` | Administrative | General healthcare admin | Various |

## UB-Series (Unilateral Benefits)

| BUC | Name | Purpose |
|-----|------|---------|
| `UB_BUC_01` | Unilateral benefit notification | Benefits outside coordination |

## BUC Lifecycle

```
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Created │───►│  Open   │───►│ Closed  │
│         │    │ (active)│    │         │
└─────────┘    └─────────┘    └─────────┘
                    │
                    ▼
              SEDs can be
              added while open
```

### BUC States

| State | Description | Can Add SEDs |
|-------|-------------|--------------|
| Open | Active case | Yes |
| Closed | Completed | No |
| Cancelled | Voided | No |

### Check BUC Status
```kotlin
val erÅpen = eessiService.erBucAapen(rinaSaksnummer)
val kanOpprettes = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer)
```

## BUC Type Enum

**Location**: `domain/src/main/kotlin/no/nav/melosys/domain/eessi/BucType.kt`

```kotlin
enum class BucType {
    LA_BUC_01, LA_BUC_02, LA_BUC_03, LA_BUC_04, LA_BUC_05, LA_BUC_06,
    H_BUC_01, H_BUC_02, H_BUC_03, H_BUC_04, H_BUC_05, H_BUC_06, H_BUC_07, H_BUC_10,
    UB_BUC_01;

    companion object {
        fun fraBestemmelse(bestemmelse: LovvalgBestemmelse): BucType
        fun fraEftaBestemmelse(bestemmelse: LovvalgBestemmelse): BucType
    }
}
```

## EFTA/GB Convention

After Brexit, UK uses EFTA convention with different BUC mapping:

```kotlin
// Special handling for UK
if (land == Land_iso2.GB) {
    bucType = BucType.fraEftaBestemmelse(bestemmelse)
}
```

Key differences:
- Different BUC types for same situations
- Special text content in SEDs
- Separate institution registry

## Saksrelasjon

Links archive case (GSAK) to RINA BUC:

```kotlin
eessiService.lagreSaksrelasjon(
    gsakSaksnummer = 123456L,
    rinaSaksnummer = "RINA-789",
    sedType = SedType.A003
)

// Later retrieval
val bucer = eessiService.hentTilknyttedeBucer(gsakSaksnummer)
```

## Creating BUC

### New BUC
```kotlin
// Creates BUC and first SED
val result = eessiService.opprettBucOgSed(
    behandling = behandling,
    sedType = SedType.A003,
    mottakerinstitusjoner = listOf("SE:FK"),
    journalpostID = journalpostId
)
// Returns RINA case number
```

### Add SED to Existing BUC
```kotlin
// Add follow-up SED
eessiService.sendSedPåEksisterendeBuc(
    behandling = behandling,
    sedType = SedType.A011,
    rinaSaksnummer = existingRinaCase
)
```

## Mottakerinstitusjoner (Recipient Institutions)

Each BUC type + country combination has valid recipients:

```kotlin
val institusjoner = eessiService.hentMottakerinstitusjoner(
    bucType = BucType.LA_BUC_01,
    landkode = "SE"
)
// Returns list of institution IDs like ["SE:FK", "SE:SKV"]
```

