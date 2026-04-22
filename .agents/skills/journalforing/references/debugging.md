# Journalføring Debugging Guide

## Common Issues

### Issue: Journalpost Already Finalized

**Symptom**:
```
409 Conflict: Journalposten er allerede ferdigstilt
```

**Cause**: Attempting to finalize a journalpost that's already in FERDIGSTILT status

**Investigation**:
```kotlin
val jp = safConsumer.hentJournalpost(journalpostId)
log.info("Status: ${jp?.journalstatus}")
// If FERDIGSTILT, skip finalization
```

**Resolution**: Check status before finalization in code

### Issue: Missing Avsender

**Symptom**:
```
FunksjonellException: "Både avsenderID og AvsenderNavn er null. AvsenderNavn er påkrevd for å journalføre."
```

**Cause**: Incoming journalpost without sender information

**Investigation**:
```kotlin
val avsenderID = prosessinstans.getData(ProsessDataKey.AVSENDER_ID)
val avsenderNavn = prosessinstans.getData(ProsessDataKey.AVSENDER_NAVN)
val erElektronisk = prosessinstans.getMottakskanalErElektronisk()
log.info("AvsenderID: $avsenderID, Navn: $avsenderNavn, Elektronisk: $erElektronisk")
```

**Resolution**:
- For electronic submissions: avsenderNavn can be derived from system
- For paper: avsenderNavn must be provided manually

### Issue: Cannot Find Document

**Symptom**:
```
Empty bytes when calling hentDokument
```

**Causes**:
- Wrong variantformat (ARKIV vs ORIGINAL)
- DocumentInfoId doesn't exist
- No access to document

**Investigation**:
```kotlin
val jp = safConsumer.hentJournalpost(journalpostId)
jp?.dokumenter?.forEach { dok ->
    log.info("Dokument: ${dok.dokumentInfoId}, tittel: ${dok.tittel}")
    dok.dokumentvarianter.forEach { variant ->
        log.info("  Variant: ${variant.variantformat}, tilgang: ${variant.saksbehandlerHarTilgang}")
    }
}
```

**Resolution**: Use correct variantformat (usually ARKIV for viewing)

### Issue: Journalpost Not Linked to Case

**Symptom**: Document not appearing in case's document list

**Investigation**:
```kotlin
// Check if journalpost has sak set
val jp = safConsumer.hentJournalpost(journalpostId)
log.info("Sak: ${jp?.sak?.fagsakId}, system: ${jp?.sak?.fagsaksystem}")

// Check all journalposts for case
val docs = safConsumer.hentDokumentoversikt(saksnummer)
log.info("Found ${docs.size} journalposts for $saksnummer")
```

**Resolution**: Update journalpost with correct sak reference

### Issue: Wrong Tema

**Symptom**: Access denied or document not visible to Melosys

**Cause**: Journalpost created with wrong tema

**Investigation**:
```kotlin
val jp = safConsumer.hentJournalpost(journalpostId)
log.info("Tema: ${jp?.tema}")
// Should be MED, TRY, or UFM for Melosys
```

**Resolution**: Update journalpost tema before finalization

### Issue: Saga Step Fails

**Symptom**: `OppdaterOgFerdigstillJournalpost` step fails

**Investigation**:
```sql
-- Check prosessinstans state
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status, pi.data
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId;

-- Look for required data keys:
-- JOURNALPOST_ID, DOKUMENT_ID, BRUKER_ID
-- AVSENDER_ID, AVSENDER_NAVN, AVSENDER_TYPE
-- MOTTATT_DATO, HOVEDDOKUMENT_TITTEL
```

**Common missing data**:
- `JOURNALPOST_ID` not set from initial reception
- `BRUKER_ID` not resolved from PDL

## Log Patterns

### JoarkService Operations
```bash
grep "JoarkService\|joarkFasade" application.log
```

### SafConsumer Calls
```bash
grep "SafConsumer\|graphql" application.log
```

### Saga Step
```bash
grep "OppdaterOgFerdigstillJournalpost\|OPPDATER_OG_FERDIGSTILL" application.log
```

### Specific Journalpost
```bash
grep "journalpostId=12345\|journalpost 12345" application.log
```

## SQL Queries

### Find Saksopplysning for Documents
```sql
-- Documents are stored as saksopplysning
SELECT so.id, so.type, so.verdi, so.behandling_id
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'DOKUMENT';
```

### Check Behandling Journalpost Link
```sql
-- Find if behandling has journalpost via prosessinstans data
SELECT pi.data
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.data LIKE '%JOURNALPOST_ID%';
```

### Find Stuck Prosessinstans
```sql
-- Find prosessinstans stuck at journalføring step
SELECT pi.id, pi.behandling_id, pi.type, pi.status, pi.registrert_dato
FROM prosessinstans pi
WHERE pi.sist_utforte_steg = 'OPPDATER_OG_FERDIGSTILL_JOURNALPOST'
AND pi.status = 'FEILET'
ORDER BY pi.registrert_dato DESC;
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `integrasjon/.../joark/JoarkService.java:oppdaterOgFerdigstillJournalpost` |
| SAF Consumer | `integrasjon/.../joark/saf/SafConsumer.java` |
| Saga Step | `saksflyt/.../steg/jfr/OppdaterOgFerdigstillJournalpost.kt:utfør` |
| Request Validator | `integrasjon/.../joark/JournalpostRequestValidator.java` |

## Manual Operations

### Re-run Saga Step
```kotlin
// Via prosessinstans service
prosessinstansService.restartProsessinstans(prosessinstansId)
```

### Update Journalpost Manually
```kotlin
// Direct API call if saga failed
val oppdatering = JournalpostOppdatering.Builder()
    .medSaksnummer(saksnummer)
    .medBrukerID(fnr)
    .medTema("MED")
    .build()
joarkService.oppdaterOgFerdigstillJournalpost(journalpostId, oppdatering)
```

### Verify Journalpost State
```kotlin
// Check current state
val jp = safConsumer.hentJournalpost(journalpostId)
println("Status: ${jp?.journalstatus}")
println("Sak: ${jp?.sak?.fagsakId}")
println("Bruker: ${jp?.bruker?.id}")
println("Tema: ${jp?.tema}")
```

## Environment Differences

| Aspect | Dev (Q1/Q2) | Prod |
|--------|-------------|------|
| Joark URL | dokarkiv-q1/q2.dev-fss-pub | dokarkiv.prod-fss-pub |
| SAF URL | saf-q1/q2.dev-fss-pub | saf.prod-fss-pub |
| Mock | melosys-mock in local | N/A |

## Mock Setup

In local development, Joark/SAF are mocked:
- Joark mock in `melosys-docker-compose/mock`
- Returns successful responses
- Stores journalposts in-memory (not persistent)
