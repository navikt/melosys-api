---
name: journalforing
description: |
  Expert knowledge of Journalføring (document archiving) in melosys-api.
  Use when: (1) Creating/updating journalposts (Joark API),
  (2) Retrieving documents from SAF (GraphQL),
  (3) Understanding document workflows,
  (4) Debugging journalpost issues,
  (5) Mapping document types and categories.
  Triggers: "journalpost", "journalføring", "Joark", "dokarkiv", "SAF",
  "dokumentoversikt", "hentDokument", "ferdigstill journalpost", "Variantformat".
---

# Journalføring System

Journalføring handles document archiving through Joark (create/update) and SAF (read/query).
All documents in melosys are archived as journalposts linked to a fagsak.

## Quick Reference

### Module Structure
```
integrasjon/joark/
├── JoarkService.java              # Main service (creates, updates, finalizes)
├── JoarkFasade.java               # Interface for service layer
├── JournalpostOppdatering.java    # Update request builder
├── JournalpostRequestValidator.java
├── DokumentKategoriKode.java      # Document category codes
├── Variantformat.java             # ARKIV, ORIGINAL
├── journalpostapi/                # Joark REST API
│   └── dto/                       # Request/response DTOs
└── saf/                           # SAF GraphQL API
    ├── SafClient.java             # GraphQL client
    └── dto/                       # Response DTOs
        └── journalpost/           # Journalpost domain objects

saksflyt/steg/jfr/
└── OppdaterOgFerdigstillJournalpost.kt  # Saga step

domain/arkiv/
├── Journalposttype.kt             # INN, UT, NOTAT
├── ArkivDokument.kt               # Document domain model
├── FysiskDokument.kt              # Physical document
├── Vedlegg.kt                     # Attachments
└── DokumentReferanse.kt           # Document reference
```

### Core Concepts

**Two APIs:**

| API | Purpose | Protocol |
|-----|---------|----------|
| **Joark** (dokarkiv) | Create, update, finalize journalposts | REST |
| **SAF** | Query, read documents | GraphQL |

**Journalpost Types:**

| Type | Code | Description |
|------|------|-------------|
| `INN` | I | Incoming document (søknad, brev fra bruker) |
| `UT` | U | Outgoing document (vedtak, brev til bruker) |
| `NOTAT` | N | Internal note |

**Journalpost Status Flow:**
```
MOTTATT → UNDER_ARBEID → JOURNALFOERT → FERDIGSTILT → EKSPEDERT
                                    ↘ UTGAAR (if expired)
                                    ↘ FEILREGISTRERT (if error)
```

## Key Operations

### Create Journalpost (Outgoing Document)
```kotlin
// Entry point: JoarkService.opprettJournalpost(OpprettJournalpost, forsøkEndeligJfr)
// Build the domain OpprettJournalpost (tittel, tema "MED", journalposttype "U",
// bruker, sak (fagsaksystem "MELOSYS"), dokumenter), then:
val journalpostId: String = joarkService.opprettJournalpost(opprettJournalpost, forsøkEndeligJfr)
// JoarkService maps to OpprettJournalpostRequest.av(...) and calls
// journalpostapiClient.opprettJournalpost(request, forsøkEndeligJfr),
// returning OpprettJournalpostResponse.journalpostId.
```

### Finalize Journalpost (Incoming)
```kotlin
// After attaching to case, finalize
joarkService.oppdaterOgFerdigstillJournalpost(
    journalpostId = "12345",
    oppdatering = JournalpostOppdatering.Builder()
        .medSaksnummer(saksnummer)
        .medBrukerID(fnr)
        .medTema("MED")
        .medTittel("Søknad om A1")
        .build()
)
```

### Query Documents from SAF
```kotlin
// GraphQL query for documents linked to case
val dokumenter = safClient.hentDokumentoversikt(saksnummer)

// Get specific journalpost
val journalpost = safClient.hentJournalpost(journalpostId)

// Download document content (always ARKIV variant; no variantformat arg)
val bytes = safClient.hentDokument(journalpostId, dokumentInfoId)
```

### Saga Step: Finalize After Behandling
```kotlin
// OppdaterOgFerdigstillJournalpost.utfør()
// Reads from ProsessDataKey:
// - JOURNALPOST_ID, DOKUMENT_ID
// - BRUKER_ID, AVSENDER_ID, AVSENDER_NAVN
// - MOTTATT_DATO, HOVEDDOKUMENT_TITTEL
// - FYSISKE_VEDLEGG, LOGISKE_VEDLEGG_TITLER
```

## Document Categories

`DokumentKategoriKode` values:

| Code | Description |
|------|-------------|
| `SOK` | Søknad |
| `VB` | Vedtaksbrev |
| `B` | Brev |
| `SED` | Structured Electronic Document (EU) |
| `SYS_SED` | System-generated SED |
| `KA` | Klage eller anke |
| `ES` | Elektronisk skjema |
| `EP` | E-post |
| `IB` | Infobrev |
| `FORVALTNINGSNOTAT` | Internal note |
| `PUBL_BLANKETT_EOS` | EU form |

## Variant Formats

| Format | Description |
|--------|-------------|
| `ARKIV` | Archived version (PDF/PDFA) |
| `ORIGINAL` | Original format (XML/JSON metadata) |

## Integration Flow

### Incoming Document (Søknad)
```
1. Document received (MOTTATT)
2. JoarkService.hentJournalpost() - fetch metadata
3. Create behandling, link to fagsak
4. JoarkService.oppdaterOgFerdigstillJournalpost()
   - Updates: bruker, sak, tema, tittel, avsender
   - Status: MOTTATT → FERDIGSTILT
```

### Outgoing Document (Vedtak)
```
1. DokumentService produces document
2. JoarkService.opprettJournalpost()
   - Creates with status FERDIGSTILT
   - Returns journalpostId, dokumentInfoId
3. Distribution to recipient
```

## Common Issues

### Issue: Journalpost Not Found
**Symptom**: `IkkeFunnetException` when fetching journalpost

**Causes**:
- Wrong journalpostId
- Journalpost in different environment
- Access denied (tema mismatch)

**Debug**:
```kotlin
val jp = safClient.hentJournalpost(journalpostId)
log.info("Status: ${jp?.journalstatus}, Tema: ${jp?.tema}")
```

### Issue: Cannot Finalize
**Symptom**: Error when calling `ferdigstillJournalføring`

**Check**:
1. Journalpost already finalized (FERDIGSTILT)?
2. Missing required fields (bruker, sak)?
3. Wrong tema for the case type?

### Issue: Missing Document Content
**Symptom**: Empty bytes from `hentDokument`

**Check**:
- Document exists in journalpost (correct `dokumentInfoId`)
- ARKIV variant is present (`hentDokument` only fetches the ARKIV variant)
- Access permissions (`saksbehandlerHarTilgang`)

## SQL Queries

### Check Journalpost on Behandling
```sql
-- Journalposts are NOT stored as a saksopplysning. The journalpost reference
-- is carried in the saga's prosessinstans data (JOURNALPOST_ID, DOKUMENT_ID).
SELECT pi.data
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.data LIKE '%JOURNALPOST_ID%';
-- To list documents for a case, query SAF instead (see below).
```

### Find Documents by Saksnummer
```sql
-- Via SAF GraphQL, not local DB
-- Use: safClient.hentDokumentoversikt(saksnummer)
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `integrasjon/.../joark/JoarkService.java` |
| Facade Interface | `integrasjon/.../joark/JoarkFasade.java` |
| Joark write client | `integrasjon/.../joark/journalpostapi/JournalpostapiClient.kt` |
| SAF Client | `integrasjon/.../joark/saf/SafClient.java` |
| Saga Step | `saksflyt/.../steg/jfr/OppdaterOgFerdigstillJournalpost.kt` |
| Document Domain | `domain/.../arkiv/*.kt` |
| Category Codes | `integrasjon/.../joark/DokumentKategoriKode.java` |

## Detailed Documentation

- **[Joark API](references/joark-api.md)**: Creating and updating journalposts
- **[SAF API](references/saf.md)**: Querying and reading documents
- **[Debugging](references/debugging.md)**: Common journalføring issues, log patterns, and SQL
