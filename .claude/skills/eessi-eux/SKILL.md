---
skill: eessi-eux
description: Expert knowledge of EESSI integration via EUX/RINA for cross-border social security
triggers:
  - eessi
  - eux
  - rina
  - buc
  - sed
  - electronic exchange
  - social security exchange
  - a001
  - a003
  - la_buc
references:
  - references/buc-types.md
  - references/sed-types.md
  - references/api.md
  - references/debugging.md
---

# EESSI-EUX Skill

## Quick Reference

### What is EESSI?

EESSI (Electronic Exchange of Social Security Information) is the EU system for electronic exchange of social security data between member states. Melosys integrates with EESSI through:

- **EUX** - Norwegian middleware/facade for RINA (managed by Team EUX)
- **RINA** - The EU's case management system for EESSI
- **melosys-eessi** - Melosys's internal facade service for EUX

### Architecture Overview

```
                    ┌─────────────────┐
                    │   RINA (EU)     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │     EUX         │   (Team EUX)
                    │  (NAV Facade)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  melosys-eessi  │   (Team Melosys)
                    └────────┬────────┘
                             │
┌──────────────────────┐     │     ┌──────────────────────┐
│    Kafka Topic       │◄────┴────►│    REST API          │
│ (incoming SED)       │           │ (outgoing operations)│
└──────────────────────┘           └──────────────────────┘
           │                                  │
           ▼                                  ▼
┌──────────────────────┐           ┌──────────────────────┐
│ EessiMeldingConsumer │           │   EessiConsumer      │
└──────────────────────┘           └──────────────────────┘
           │                                  │
           └─────────────┬────────────────────┘
                         │
                ┌────────▼────────┐
                │  EessiService   │
                └─────────────────┘
```

### Key Components

| Component | Description | Location |
|-----------|-------------|----------|
| `EessiConsumer` | REST client interface to melosys-eessi | integrasjon/.../eessi/ |
| `EessiConsumerImpl` | WebClient implementation | integrasjon/.../eessi/ |
| `EessiService` | Main service for EESSI operations | service/.../dokument/sed/ |
| `EessiMeldingConsumer` | Kafka consumer for incoming SED | service/.../eessi/kafka/ |
| `SedRuter` | Routes incoming SED to processing | service/.../eessi/ruting/ |
| `SedDataBygger` | Builds SED data payload | service/.../dokument/sed/ |

### BUC Types (Business Use Cases)

**LA_BUC** - Lovvalg (Applicable Legislation):

| BUC | Purpose | Initiating SED |
|-----|---------|----------------|
| LA_BUC_01 | Art. 16 - Exception agreements | A001 |
| LA_BUC_02 | Art. 13 - Work in multiple countries | A003 |
| LA_BUC_03 | Notification of applicable legislation | A005 |
| LA_BUC_04 | Art. 12 - Posted workers | A003 |
| LA_BUC_05 | Art. 11 - General determination | A005 |
| LA_BUC_06 | Family member membership | A003 |

**H_BUC** - Helytelser (Health benefits):
Various H_BUC types for health benefit coordination.

### SED Types (Structured Electronic Documents)

**A-series** (Applicable Legislation):

| SED | Purpose |
|-----|---------|
| A001 | Request for exception (Art. 16) |
| A002 | Refusal of exception request |
| A003 | Determination request (Art. 13) |
| A004 | Objection to determination |
| A005 | Notification of applicable legislation |
| A011 | Acceptance of exception request |
| A012 | Confirmation of applicable legislation |

**X-series** (Administrative):

| SED | Purpose |
|-----|---------|
| X001 | Request for information |
| X008 | Invalidation of previously sent SED |
| X009 | Reminder/purring |

### Core Operations

```kotlin
// Create BUC and send SED
eessiConsumer.opprettBucOgSed(
    sedDataDto,      // SED content
    vedlegg,         // Attachments
    bucType,         // LA_BUC_02, etc.
    sendAutomatisk,  // true = send immediately
    oppdaterEksisterende // true = update if exists
)

// Send SED on existing BUC
eessiConsumer.sendSedPåEksisterendeBuc(
    sedDataDto,
    rinaSaksnummer,  // RINA case ID
    sedType          // A012, A004, etc.
)

// Get recipient institutions
eessiConsumer.hentMottakerinstitusjoner(
    bucType,         // "LA_BUC_02"
    landkoder        // Set("DE", "SE")
)
```

### Incoming SED Flow

```
1. melosys-eessi receives SED from EUX
           │
           ▼
2. Publishes to Kafka topic
           │
           ▼
3. EessiMeldingConsumer.mottaMeldingAiven()
           │
           ▼
4. ProsessinstansService.opprettProsessinstansSedMottak()
           │
           ▼
5. SedRuter.rutSedTilBehandling() - routes to correct handler:
   ├── AnmodningOmUnntakSedRuter (A001)
   ├── ArbeidFlereLandSedRuter (A003)
   ├── SvarAnmodningUnntakSedRuter (A011/A002)
   └── DefaultSedRuter (others)
           │
           ▼
6. Creates/updates behandling based on SED content
```

### Outgoing SED Flow

```
1. Vedtak fattet (decision made)
           │
           ▼
2. IVERKSETT_VEDTAK prosessinstans starts
           │
           ▼
3. AbstraktSendUtland.sendUtland()
           │
           ▼
4. EessiService.opprettOgSendSed()
           │
           ▼
5. SedDataBygger builds SED payload
           │
           ▼
6. EessiConsumer.opprettBucOgSed() / sendSedPåEksisterendeBuc()
           │
           ▼
7. melosys-eessi → EUX → RINA
```

### Quick Debugging

```sql
-- Find all SEDs for a fagsak
SELECT sd.*, f.saksnummer
FROM sed_dokument sd
JOIN fagsak f ON sd.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer;

-- Check RINA case relationship
SELECT saksrelasjon_id, rina_saksnummer, gsak_saksnummer, buc_type
FROM saksrelasjon
WHERE gsak_saksnummer = :gsakSaksnummer;
```

## When to Use This Skill

- Understanding BUC lifecycle and SED exchange
- Debugging SED sending/receiving issues
- Understanding which BUC/SED applies to which lovvalg article
- Investigating EUX API errors
- Understanding institution catalog lookup
- Troubleshooting Kafka message processing

## Related Skills

- **lovvalg**: Law determination that triggers SED sending
- **sed**: Detailed SED document types and content
- **saksflyt**: Saga steps for SED operations
- **behandling**: Behandlinger created from incoming SEDs
