---
name: journalforing
description: |
  Expert knowledge of Journalføring (document archiving) in melosys-api.
  Use when: (1) Creating/updating journalposts (Joark API),
  (2) Retrieving documents from SAF (GraphQL),
  (3) Understanding document workflows,
  (4) Debugging journalpost issues,
  (5) Mapping document types and categories.
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
    ├── SafConsumer.java           # GraphQL client
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
// JoarkService.opprettJournalpost
val request = OpprettJournalpostRequest(
    tittel = "Vedtak om lovvalg",
    tema = "MED",
    journalposttype = "U",
    bruker = Bruker(id = fnr, idType = "FNR"),
    sak = Sak(sakstype = "FAGSAK", fagsakId = saksnummer, fagsaksystem = "MELOSYS"),
    dokumenter = listOf(dokument)
)
val response = journalpostapiConsumer.opprettJournalpost(request)
// Returns: journalpostId, dokumenter[].dokumentInfoId
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
val dokumenter = safConsumer.hentDokumentoversikt(saksnummer)

// Get specific journalpost
val journalpost = safConsumer.hentJournalpost(journalpostId)

// Download document content
val bytes = safConsumer.hentDokument(journalpostId, dokumentInfoId, "ARKIV")
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
val jp = safConsumer.hentJournalpost(journalpostId)
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
- Correct `variantformat` (ARKIV vs ORIGINAL)
- Document exists in journalpost
- Access permissions

## SQL Queries

### Check Journalpost on Behandling
```sql
-- Find journalpost linked via saksopplysning
SELECT so.* FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'DOKUMENT';
```

### Find Documents by Saksnummer
```sql
-- Via SAF GraphQL, not local DB
-- Use: safConsumer.hentDokumentoversikt(saksnummer)
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `integrasjon/.../joark/JoarkService.java` |
| Facade Interface | `integrasjon/.../joark/JoarkFasade.java` |
| SAF Consumer | `integrasjon/.../joark/saf/SafConsumer.java` |
| Saga Step | `saksflyt/.../steg/jfr/OppdaterOgFerdigstillJournalpost.kt` |
| Document Domain | `domain/.../arkiv/*.kt` |
| Category Codes | `integrasjon/.../joark/DokumentKategoriKode.java` |

## Detailed Documentation

- **[Joark API](references/joark-api.md)**: Creating and updating journalposts
- **[SAF API](references/saf.md)**: Querying and reading documents
- **[Debugging](references/debugging.md)**: Troubleshooting guide
