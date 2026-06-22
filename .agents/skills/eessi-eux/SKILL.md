---
name: eessi-eux
description: |
  Expert knowledge of EESSI integration via EUX/RINA in melosys-api.
  Use when: (1) Understanding BUC lifecycle and SED exchange (A-series A001-A012,
  X-series X001-X013, plus H_BUC/UB_BUC),
  (2) Debugging SED sending/receiving via EessiClient (opprettBucOgSed,
  sendSedPåEksisterendeBuc, X008 invalidation, X009 purring),
  (3) Understanding LA_BUC types and their purposes,
  (4) Investigating EUX API errors, RINA case (rina saksnummer / saksrelasjon) lookup,
  (5) Understanding mottakerinstitusjoner / institution catalog lookup.
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
│ EessiMeldingConsumer │           │   EessiClient        │
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
| `EessiClient` | WebClient REST client to melosys-eessi (`open class ... : JsonRestIntegrasjon`) | integrasjon/.../eessi/EessiClient.kt |
| `EessiService` | Main service for EESSI operations | service/.../dokument/sed/ |
| `EessiMeldingConsumer` | Kafka consumer for incoming SED | service/.../eessi/kafka/ |
| `SedRuter` | Routes incoming SED to processing | service/.../eessi/ruting/ |
| `SedDataBygger` | Builds SED data payload | service/.../dokument/sed/ |

### BUC Types (Business Use Cases)

**LA_BUC** - Lovvalg (Applicable Legislation):

| BUC | Purpose | Initiating SED |
|-----|---------|----------------|
| LA_BUC_01 | Art. 16 - Exception agreements (Anmodning om unntak) | A001 |
| LA_BUC_02 | Art. 13 - Work in multiple countries (Beslutning om lovvalg) | A003 |
| LA_BUC_03 | Melding om relevant informasjon | A008 |
| LA_BUC_04 | Art. 12 - Posted workers (Melding om utstasjonering) | A009 |
| LA_BUC_05 | Art. 11 - General determination (Melding om lovvalg) | A010 |
| LA_BUC_06 | Forespørsel om mer informasjon | A005 (+ A006 reply) |

> `BucType.fraBestemmelse()` only ever maps to LA_BUC_01, LA_BUC_02, LA_BUC_04 and
> LA_BUC_05 (see `BucType.kt`). LA_BUC_03 and LA_BUC_06 exist in the `BucType` enum
> but are not produced from a lovvalgsbestemmelse — they are info/notification BUCs.

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
| A005 | Request for more information (Anmodning om mer informasjon) |
| A006 | Reply to request for more information (Svar på anmodning) |
| A008 | Notification of relevant information (Melding om relevant informasjon) |
| A009 | Notification of posting, Art. 12 (Melding om utstasjonering) |
| A010 | Notification of applicable legislation, Art. 11 (Melding om lovvalg) |
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
eessiClient.opprettBucOgSed(
    sedDataDto,      // SED content
    vedlegg,         // Attachments
    bucType,         // LA_BUC_02, etc.
    sendAutomatisk,  // true = send immediately
    oppdaterEksisterendeOmFinnes // true = update if exists
)

// Send SED on existing BUC
eessiClient.sendSedPåEksisterendeBuc(
    sedDataDto,
    rinaSaksnummer,  // RINA case ID
    sedType          // A012, A004, etc.
)

// Get recipient institutions
eessiClient.hentMottakerinstitusjoner(
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
6. EessiClient.opprettBucOgSed() / sendSedPåEksisterendeBuc()
           │
           ▼
7. melosys-eessi → EUX → RINA
```

### Quick Debugging

melosys-api does NOT persist SEDs or the BUC↔NAV-case relationship in its own
Oracle DB. There is no `sed_dokument` or `saksrelasjon` table here — `saksrelasjon`
exists only as `SaksrelasjonDto` and as `PROSESS_STEG` name strings (e.g.
`OPPDATER_SAKSRELASJON`). The actual saksrelasjon data lives in the **melosys-eessi**
service's own database. To trace SED activity from melosys-api, query the saga tables:

```sql
-- Find SED-send / SED-mottak saga runs for a behandling.
-- NB: column is PROSESS_TYPE (not "type") and SIST_FULLFORT_STEG (not "sist_utforte_steg").
SELECT pi.uuid, pi.prosess_type, pi.status, pi.sist_fullfort_steg, pi.endret_dato
FROM prosessinstans pi
JOIN behandling b ON pi.behandling_id = b.id
WHERE b.id = :behandlingId
  AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.endret_dato DESC;

-- The BUC↔NAV-case relationship (rina_saksnummer/gsak_saksnummer) is owned by
-- melosys-eessi. From melosys-api, look it up at runtime via
-- EessiClient.hentSakForGsakSaksnummer(gsakSaksnummer) /
-- EessiClient.hentSakForRinasaksnummer(rinaSaksnummer).
```

## When to Use This Skill

- Understanding BUC lifecycle and SED exchange
- Debugging SED sending/receiving issues
- Understanding which BUC/SED applies to which lovvalg article
- Investigating EUX API errors
- Understanding institution catalog lookup
- Troubleshooting Kafka message processing

## Reference Files

Deeper detail lives in this skill's `references/` directory:

- [references/api.md](references/api.md) — full `EessiClient` method/endpoint reference and Kafka message shapes
- [references/buc-types.md](references/buc-types.md) — BUC types, bestemmelse→BUC mapping, BUC lifecycle states
- [references/sed-types.md](references/sed-types.md) — SED types, content, routing, and SedType enum
- [references/debugging.md](references/debugging.md) — SQL queries, common error scenarios, log patterns, code entry points

## Related Skills

- **lovvalg**: Law determination that triggers SED sending
- **sed**: Detailed SED document types and content
- **saksflyt**: Saga steps for SED operations
- **behandling**: Behandlinger created from incoming SEDs
