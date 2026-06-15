# BUC Types Reference

BUC (Business Use Case) groups related SEDs into a workflow/case.

## LA-Series (Applicable Legislation)

| BUC | Name | Purpose | Key SEDs |
|-----|------|---------|----------|
| `LA_BUC_01` | Søknad om unntak (exception request, Art. 16) | Request exception from normal rules | A001, A002, A011 |
| `LA_BUC_02` | Beslutning om lovvalg / arbeid i flere land (Art. 13) | Decision, work in multiple countries | A003, A004, A007 |
| `LA_BUC_03` | (in enum, little-used in this module) | — | — |
| `LA_BUC_04` | Melding om utstasjonering (posting notification, Art. 12) | Notify about posting | A009 |
| `LA_BUC_05` | Lovvalg etter hovedregel (Art. 11) | Main-rule applicable legislation | A003 |
| `LA_BUC_06` | Forespørsel om mer informasjon | Request additional information | A005, A006 |

### BUC Selection Logic

`BucType.fraBestemmelse(bestemmelse)` dispatches on the concrete `LovvalgBestemmelse`
subtype (883/2004, tilleggsbestemmelser, and the post-Brexit EFTA/UK convention
classes), all routed through the single public `fraBestemmelse`. The 883/2004
article → BUC mapping is:

```kotlin
// BucType.kt (hentBucTypeFra883_2004) — exact article -> BUC mapping
ART_11_*, ART_15            -> LA_BUC_05  // main rule
ART_12_1, ART_12_2          -> LA_BUC_04  // posted workers (utstasjonering)
ART_13_*                    -> LA_BUC_02  // multi-state work / decision
ART_16_1, ART_16_2          -> LA_BUC_01  // exception request (søknad om unntak)
// unmapped -> IllegalArgumentException
```

EFTA/UK convention bestemmelser are handled by the same `fraBestemmelse` entry
point (private `hentBucTypeFraKonvEfta` / `hentBuctypeFraTilleggsBestemmelserKonvEfta`),
not by a separate public function.

## H-Series (Healthcare)

H_BUC types exist in the `BucType` enum for completeness, but melosys-api does not
drive an H-series healthcare flow (it is lovvalg-focused; H_BUC initiation lives
largely in melosys-eessi). The enum members are:

```
H_BUC_01, H_BUC_02a, H_BUC_02b, H_BUC_02c,
H_BUC_03a, H_BUC_03b, H_BUC_04, H_BUC_05,
H_BUC_06, H_BUC_07, H_BUC_08, H_BUC_09, H_BUC_10
```

Note there is no plain `H_BUC_02` / `H_BUC_03` — only the lettered variants
(`H_BUC_02a/b/c`, `H_BUC_03a/b`). `H_BUC_08` and `H_BUC_09` are also present.

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
```java
boolean erÅpen = eessiService.erBucAapen(arkivsakID);                          // long arkivsakID
boolean kanOpprettes = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, SedType.A012); // (rina, sedType) -> boolean
```

## BUC Type Enum

**Location**: `domain/src/main/kotlin/no/nav/melosys/domain/eessi/BucType.kt`

```kotlin
enum class BucType {
    LA_BUC_01, LA_BUC_02, LA_BUC_03, LA_BUC_04, LA_BUC_05, LA_BUC_06,
    H_BUC_01, H_BUC_02a, H_BUC_02b, H_BUC_02c, H_BUC_03a, H_BUC_03b,
    H_BUC_04, H_BUC_05, H_BUC_06, H_BUC_07, H_BUC_08, H_BUC_09, H_BUC_10,
    UB_BUC_01;

    companion object {
        // Single public entry point; dispatches on the LovvalgBestemmelse subtype,
        // including the EFTA/UK convention classes (private helpers).
        @JvmStatic
        fun fraBestemmelse(bestemmelse: LovvalgBestemmelse): BucType
    }
}
```

## EFTA/GB Convention

After Brexit, UK uses the EEA EFTA convention. There is **no** separate
`fraEftaBestemmelse` public function — convention bestemmelser
(`Lovvalgbestemmelser_konv_efta_storbritannia` /
`Tilleggsbestemmelser_konv_efta_storbritannia`) are routed through the same
`fraBestemmelse` entry point via private helpers
(`hentBucTypeFraKonvEfta`, `hentBuctypeFraTilleggsBestemmelserKonvEfta`):

```kotlin
// Same call for all bestemmelser, EFTA/UK included
val bucType = BucType.fraBestemmelse(bestemmelse)
```

For UK convention cases, `EessiService.mapYtterligereInformasjon` also appends the
text "Issued under the EEA EFTA Convention." to the SED's ytterligere informasjon.

## Saksrelasjon

Links archive case (arkivsak) to RINA BUC. Third argument is the **bucType** name:

```java
eessiService.lagreSaksrelasjon(
    arkivsakID,                   // Long
    "RINA-789",                  // String rinaSaksnummer
    BucType.LA_BUC_02.name()      // String bucType
);

// Later retrieval (arkivsakID + statuser filter; empty list = all)
List<BucInformasjon> bucer = eessiService.hentTilknyttedeBucer(arkivsakID, List.of());
```

## Creating BUC

### New BUC
```java
// Creates BUC and first SED. Takes behandlingID + BucType (the SED type is
// derived from the behandling's resultat/periode inside the service).
String rinaUrl = eessiService.opprettBucOgSed(
    behandlingID,                 // long
    BucType.LA_BUC_02,            // BucType
    List.of("SE:FK"),            // mottakerInstitusjoner
    dokumentReferanser            // Collection<DokumentReferanse>
);
// Returns the RINA URL
```

### Add SED to Existing BUC
EessiService exposes purpose-specific send methods (e.g. `sendGodkjenningArbeidFlereLand`,
`sendAnmodningUnntakSvar`, `sendAvslagUtpekingSvar`). These internally call
`eessiClient.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, sedType)` — that
method lives on `EessiClient`, not `EessiService`:

```java
eessiService.sendGodkjenningArbeidFlereLand(behandlingID, ytterligereInformasjon); // sends A012
```

## Mottakerinstitusjoner (Recipient Institutions)

Each BUC type + country combination has valid recipients. The method takes the
bucType **name** and a collection of landkoder:

```java
List<Institusjon> institusjoner = eessiService.hentEessiMottakerinstitusjoner(
    BucType.LA_BUC_01.name(),     // String bucType
    Set.of("SE")                  // Collection<String> landkoder
);
```

