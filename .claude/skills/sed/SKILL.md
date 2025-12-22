---
name: sed
description: |
  Expert knowledge of SED/EESSI integration in melosys-api.
  Use when: (1) Understanding SED document types (A001, A003, H-series, X008),
  (2) Debugging SED sending/receiving flows and RINA integration,
  (3) Working with BUC (Business Use Case) creation and management,
  (4) Understanding saga steps for SED handling (SEND_VEDTAK_UTLAND, SED_MOTTAK_RUTING),
  (5) Troubleshooting mottakerinstitusjoner (recipient institutions) issues.
---

# SED/EESSI Integration

SED (Structured Electronic Document) is the EU standard for exchanging social security information.
EESSI (Electronic Exchange of Social Security Information) is the system that handles this exchange.
Melosys integrates via melosys-eessi service which communicates with RINA.

## Quick Reference

### Module Structure
```
integrasjon/eessi/
├── EessiConsumer.java          # Interface for EESSI operations
├── EessiConsumerImpl.kt        # WebClient REST implementation
└── dto/                        # Request/response DTOs

service/dokument/sed/
├── EessiService.java           # Main business logic (450+ lines)
├── bygger/
│   └── SedDataBygger.java      # Constructs SedDataDto from treatment
├── SedTypeTilBehandlingstemaMapper.java
└── SedDataGrunnlagFactory.java

domain/eessi/
├── SedType.kt                  # 50+ SED type definitions
├── BucType.kt                  # BUC types with bestemmelse mapping
└── MelosysEessiMelding.kt      # Incoming message wrapper

saksflyt/steg/sed/
├── SendVedtakUtland.kt         # Send decision abroad
├── SendAnmodningOmUnntak.kt    # Send exception request
├── SedMottakRuting.kt          # Route incoming SEDs
├── OpprettSedGrunnlag.kt       # Map SED to mottatteopplysninger
└── ...                         # 14+ SED-related steps
```

### Key SED Types

**A-Series (Applicable Legislation)**:
| SED | Purpose | When Used |
|-----|---------|-----------|
| `A001` | Request for applicable legislation | Anmodning om unntak (Art. 16) |
| `A003` | Certificate of applicable legislation | Vedtak utland (A1) |
| `A004` | Refusal of certificate | Avslag |
| `A009` | Pension reminder (Purring) | Follow-up |
| `A011` | Approval of request | Godkjenning anmodning |
| `A012` | Information about changes | Endringsmelding |

**H-Series (Healthcare)**:
| SED | Purpose |
|-----|---------|
| `H001` | Request for healthcare coverage |
| `H003` | Healthcare entitlement certificate |
| `H020` | Insurance period statement |
| `H121` | Healthcare cost claim |

**X-Series (Administrative)**:
| SED | Purpose |
|-----|---------|
| `X008` | Invalidation/cancellation |
| `X009` | Reminder/follow-up |

### BUC Types (Business Use Cases)

BUC groups related SEDs into a case workflow:

| BUC | Purpose | Common SEDs |
|-----|---------|-------------|
| `LA_BUC_01` | Applicable legislation request | A001, A003, A004, A011 |
| `LA_BUC_02` | Posted worker notification | A003 |
| `LA_BUC_04` | Exception agreement (Art. 16) | A001, A011 |
| `LA_BUC_06` | Certificate correction | A003, X008 |
| `H_BUC_01` | Healthcare coordination | H001, H003 |
| `UB_BUC_01` | Unilateral benefits | Various |

## Sending SEDs

### Flow Overview
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────┐
│ Behandling  │───►│ EessiService│───►│melosys-eessi│───►│ RINA │
│ (vedtak)    │    │ (build SED) │    │ (REST call) │    │      │
└─────────────┘    └─────────────┘    └─────────────┘    └──────┘
```

### Key Operations

**Create BUC and Send SED**:
```kotlin
// Via EessiService
eessiService.opprettBucOgSed(
    behandling = behandling,
    sedType = SedType.A003,
    mottakerinstitusjoner = listOf("SE:FK"),
    journalpostID = journalpostId
)
```

**Send SED on Existing BUC**:
```kotlin
eessiService.sendSedPåEksisterendeBuc(
    behandling = behandling,
    sedType = SedType.A011,
    rinaSaksnummer = "12345"
)
```

**Get Recipient Institutions**:
```kotlin
val institusjoner = eessiService.hentMottakerinstitusjoner(
    bucType = BucType.LA_BUC_01,
    landkode = "SE"
)
```

### SedDataBygger

Constructs SED payload from treatment data:

```kotlin
// Builds complete SedDataDto
val sedData = sedDataBygger.build(behandling, sedType)

// Contains:
// - Bruker (person info)
// - Adresser (contact, residence, temporary)
// - Arbeidssted/Virksomhet (workplace/company)
// - Lovvalgsperiode (law choice period)
// - VedtakDto (decision)
```

## Receiving SEDs

### Flow Overview
```
┌──────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ RINA │───►│ Journalpost │───►│ Saga Steps  │───►│ Behandling  │
│      │    │ (EESSI)     │    │ (routing)   │    │ (created)   │
└──────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Saga Steps for Receiving

| Step | Purpose |
|------|---------|
| `SED_MOTTAK_RUTING` | Determine handling strategy |
| `OPPRETT_SED_GRUNNLAG` | Map SED to mottatteopplysninger |
| `OPPRETT_SEDDOKUMENT` | Store SED reference |
| `SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH` | Create case if new |
| `SED_MOTTAK_OPPRETT_NY_BEHANDLING` | Create new treatment |
| `BESTEM_BEHANDLINGMÅTE_SED` | Determine next steps |

### SED Type → Treatment Topic Mapping

```kotlin
// SedTypeTilBehandlingstemaMapper
A001 → Behandlingstema.LOVVALG  // Exception request
A003 → Behandlingstema.LOVVALG  // Certificate
H001 → Behandlingstema.HELSE    // Healthcare
```

## RINA Integration

### Saksrelasjon (Case Relationship)

Links GSAK (archive case) to RINA case:

```kotlin
eessiService.lagreSaksrelasjon(
    gsakSaksnummer = fagsak.gsakSaksnummer,
    rinaSaksnummer = rinaCaseId,
    sedType = SedType.A003
)
```

### BUC Status Checks

```kotlin
// Check if BUC is open
val erÅpen = eessiService.erBucAapen(rinaSaksnummer)

// Check available SED types
val kanOpprettes = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer)

// Get related BUCs
val bucer = eessiService.hentTilknyttedeBucer(gsakSaksnummer)
```

### LåsReferanse for Concurrency

SEDs use RINA case number as lock reference:

```kotlin
// In Prosessinstans
låsReferanse = LåsReferanseRinasak(rinaSaksnummer)

// Prevents concurrent operations on same RINA case
```

## Common Issues

### 1. Missing Mottakerinstitusjoner

**Symptom**: `FunksjonellException: "Fant ingen mottakerinstitusjoner for ..."`

**Cause**: Country not EESSI-ready or wrong BUC type

**Investigation**:
```kotlin
val institusjoner = eessiService.hentMottakerinstitusjoner(bucType, landkode)
// Empty list = country not configured
```

### 2. Invalid SED Type for BUC

**Symptom**: Cannot create SED on existing BUC

**Investigation**:
```kotlin
val tilgjengelige = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer)
// Check if desired SED type is in list
```

### 3. GB/EFTA Convention Special Handling

**Symptom**: Wrong BUC type or text for UK cases

**Note**: UK uses EFTA convention after Brexit, requires special mapping:
- Different BUC types
- Special text in SED content

### 4. PDF Generation Failure

**Symptom**: SED created but no PDF

**Cause**: Incomplete data in SedDataDto

**Investigation**: Check SedDataBygger for required fields

## Saga Steps Reference

### Sending Steps
| Step | Description |
|------|-------------|
| `SEND_VEDTAK_UTLAND` | Send A003 decision abroad |
| `SEND_ANMODNING_OM_UNNTAK` | Send A001 exception request |
| `SEND_SVAR_ANMODNING_UNNTAK` | Send A011/A004 response |
| `VIDERESEND_SØKNAD` | Forward application |
| `UTPEKING_SEND_AVSLAG` | Send designation refusal |
| `SEND_GODKJENNING_REGISTRERING_UNNTAK` | Send approval |

### Receiving Steps
| Step | Description |
|------|-------------|
| `SED_MOTTAK_RUTING` | Route incoming SED |
| `OPPRETT_SED_GRUNNLAG` | Create data from SED |
| `OPPRETT_SEDDOKUMENT` | Store SED reference |
| `SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH` | Create new case |
| `BESTEM_BEHANDLINGMÅTE_SED` | Determine automation |

## Debugging

### Find RINA Case for Behandling
```sql
SELECT so.verdi as rina_saksnummer
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.opplysningstype = 'RINA_SAKSNUMMER';
```

### Check SED Sending History
```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE '%SED%' OR pi.sist_utforte_steg LIKE '%SED%'
ORDER BY pi.registrert_dato DESC;
```

### Check Saksrelasjon
```sql
-- Via melosys-eessi service
-- Links GSAK to RINA cases
```

## Detailed Documentation

- **[SED Types](references/sed-types.md)**: Complete SED type reference
- **[BUC Types](references/buc-types.md)**: BUC mapping and workflows
- **[Debugging](references/debugging.md)**: SQL queries and investigation steps
