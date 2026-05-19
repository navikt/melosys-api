# Joark API Reference

## Overview

Joark (dokarkiv) is the write API for journalposts. Used for creating, updating, and finalizing documents.

## Creating Journalposts

### Outgoing Document (UT)
```kotlin
val request = OpprettJournalpostRequest(
    tittel = "Vedtak om lovvalg",
    tema = "MED",  // Medlemskap
    journalposttype = "U",
    kanal = "NAV_NO",  // or "EESSI", "SKAN_IM", etc.
    bruker = Bruker(
        id = "12345678901",
        idType = "FNR"
    ),
    avsenderMottaker = AvsenderMottaker(
        id = fnr,
        idType = "FNR",
        navn = "Ola Nordmann"
    ),
    sak = Sak(
        sakstype = "FAGSAK",
        fagsakId = "MEL-12345",
        fagsaksystem = "MELOSYS"
    ),
    dokumenter = listOf(
        Dokument(
            tittel = "Vedtak om lovvalg",
            brevkode = "NAV 02-07.05",
            dokumentKategori = "VB",
            dokumentvarianter = listOf(
                DokumentVariant(
                    filtype = "PDF",
                    variantformat = "ARKIV",
                    fysiskDokument = Base64.getEncoder().encodeToString(pdfBytes)
                )
            )
        )
    )
)

val response = journalpostapiConsumer.opprettJournalpost(request, true)
// ferdigstill = true → FERDIGSTILT
// ferdigstill = false → UNDER_ARBEID
```

### Response
```kotlin
data class OpprettJournalpostResponse(
    val journalpostId: String,
    val journalstatus: String,
    val dokumenter: List<DokumentInfo>
)

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String
)
```

## Updating Journalposts

### JournalpostOppdatering Builder
```kotlin
val oppdatering = JournalpostOppdatering.Builder()
    .medSaksnummer("MEL-12345")
    .medBrukerID("12345678901")
    .medVirksomhetOrgnr("987654321")
    .medHovedDokumentID(dokumentInfoId)
    .medTittel("Søknad om A1-attest")
    .medMottattDato(LocalDate.now())
    .medTema("MED")
    .medAvsenderID("98765432100")
    .medAvsenderNavn("Foreign Company")
    .medAvsenderType(Avsendertyper.VIRKSOMHET)
    .medAvsenderLand("SE")
    .medFysiskeVedlegg(mapOf("docId" to "Vedlegg 1"))
    .medLogiskeVedleggTitler(listOf("Arbeidsavtale", "Lønnslipp"))
    .build()
```

### Update and Finalize
```kotlin
// JoarkService.oppdaterOgFerdigstillJournalpost
joarkService.oppdaterOgFerdigstillJournalpost(journalpostId, oppdatering)

// Steps performed:
// 1. Build OppdaterJournalpostRequest
// 2. PUT /rest/journalpostapi/v1/journalpost/{id}
// 3. Finalize: PUT /rest/journalpostapi/v1/journalpost/{id}/ferdigstill
```

### OppdaterJournalpostRequest
```kotlin
data class OppdaterJournalpostRequest(
    val tema: String,
    val bruker: Bruker?,
    val sak: Sak?,
    val tittel: String?,
    val avsenderMottaker: AvsenderMottaker?,
    val dokumenter: List<Dokumentoppdatering>?,
    val tilleggsopplysninger: List<Tilleggsopplysning>?,
    val datoMottatt: LocalDate?
)
```

## Ferdigstilling (Finalization)

### Direct Finalize
```kotlin
joarkService.ferdigstillJournalføring(
    journalpostId = "12345",
    journalfoerendeEnhet = "4863"  // Melosys unit
)
```

### Finalize Request
```kotlin
data class FerdigstillJournalpostRequest(
    val journalfoerendeEnhet: String
)
// PUT /rest/journalpostapi/v1/journalpost/{id}/ferdigstill
```

## Tema Codes

| Code | Description |
|------|-------------|
| `MED` | Medlemskap |
| `TRY` | Trygdeavgift |
| `UFM` | Unntak fra medlemskap |

## Kanal Codes

| Code | Description |
|------|-------------|
| `NAV_NO` | nav.no digital submission |
| `EESSI` | EU electronic exchange |
| `SKAN_IM` | Scanning (internal) |
| `SKAN_NETS` | Scanning (external) |
| `ALTINN` | Altinn submission |

## Filetype Codes

| Type | Description |
|------|-------------|
| `PDF` | PDF document |
| `PDFA` | PDF/A archive format |
| `XML` | XML metadata |
| `JSON` | JSON metadata |
| `PNG` | PNG image |
| `JPEG` | JPEG image |

## Error Handling

### Common Errors
```kotlin
// 400 Bad Request - validation error
// 401 Unauthorized - token issue
// 403 Forbidden - access denied
// 404 Not Found - journalpost doesn't exist
// 409 Conflict - already finalized

catch (e: WebClientResponseException) {
    when (e.statusCode) {
        HttpStatus.CONFLICT -> log.warn("Already finalized: $journalpostId")
        HttpStatus.NOT_FOUND -> throw IkkeFunnetException("Journalpost $journalpostId")
        else -> throw TekniskException("Joark error: ${e.message}")
    }
}
```

## Validation Rules

### Before Finalization
- Must have `bruker` (FNR or aktørId)
- Must have `sak` linked
- Must have `tema` set
- `tittel` required on journalpost and documents
- `avsenderMottaker` required for incoming (INN)

### Document Requirements
- At least one document
- Each document needs at least ARKIV variant
- Physical document (Base64 encoded bytes)
