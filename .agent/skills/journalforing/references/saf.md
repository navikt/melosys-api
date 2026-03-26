# SAF (Sak- og dokumentflyt) API Reference

## Overview

SAF is the read API for journalposts. Uses GraphQL for queries and REST for document content retrieval.

## GraphQL Endpoint

```
POST /graphql
Content-Type: application/json
Authorization: Bearer {token}
```

## Core Queries

### Get Single Journalpost
```graphql
query hentJournalpost($journalpostId: String!) {
  journalpost(journalpostId: $journalpostId) {
    journalpostId
    tittel
    journalstatus
    tema
    journalposttype
    sak {
      fagsakId
      fagsaksystem
    }
    bruker {
      id
      type
    }
    avsenderMottaker {
      id
      type
      navn
      land
    }
    kanal
    relevanteDatoer {
      dato
      datotype
    }
    dokumenter {
      dokumentInfoId
      tittel
      brevkode
      dokumentvarianter {
        variantformat
        filtype
        saksbehandlerHarTilgang
      }
      logiskeVedlegg {
        tittel
      }
    }
  }
}
```

### Get Documents for Case
```graphql
query hentDokumentoversikt($saksnummer: String!, $tema: String) {
  dokumentoversiktFagsak(
    fagsak: {fagsakId: $saksnummer, fagsaksystem: "MELOSYS"},
    foerste: 100
  ) {
    journalposter {
      journalpostId
      tittel
      journalstatus
      journalposttype
      dokumenter {
        dokumentInfoId
        tittel
        dokumentvarianter {
          variantformat
        }
      }
    }
    sideInfo {
      finnesNesteSide
      sluttpeker
    }
  }
}
```

## SafConsumer Methods

### Fetch Journalpost
```kotlin
fun hentJournalpost(journalpostId: String): Journalpost? {
    val query = Query(
        query = JOURNALPOST_QUERY,
        variables = mapOf("journalpostId" to journalpostId)
    )
    return webClient.post()
        .uri(GRAPHQL_ROOT)
        .bodyValue(query)
        .retrieve()
        .bodyToMono<HentJournalpostResponse>()
        .block()
        ?.data?.journalpost
}
```

### Fetch Document Content
```kotlin
fun hentDokument(journalpostId: String, dokumentInfoId: String, variantformat: String): ByteArray {
    return webClient.get()
        .uri("$HENT_DOKUMENT_ROOT/$journalpostId/$dokumentInfoId/$variantformat")
        .retrieve()
        .bodyToMono<ByteArray>()
        .block() ?: ByteArray(0)
}
```

### Fetch Document Overview
```kotlin
fun hentDokumentoversikt(saksnummer: String): List<Journalpost> {
    // Returns all journalposts linked to fagsak
    // Handles pagination via sideInfo.sluttpeker
}
```

## Journalpost Domain Model

### Journalpost
```kotlin
data class Journalpost(
    val journalpostId: String,
    val tittel: String,
    val journalstatus: Journalstatus,
    val tema: String?,
    val journalposttype: Journalposttype,
    val sak: Sak?,
    val bruker: Bruker?,
    val avsenderMottaker: AvsenderMottaker?,
    val kanal: String?,
    val relevanteDatoer: List<RelevantDato>,
    val dokumenter: List<DokumentInfo>
) {
    fun erFerdigstilt(): Boolean = journalstatus == FERDIGSTILT
    fun erInngående(): Boolean = journalposttype == I
    fun erUtgående(): Boolean = journalposttype == U
    fun erNotat(): Boolean = journalposttype == N
}
```

### Journalstatus
```kotlin
enum class Journalstatus {
    MOTTATT,          // Received, not processed
    JOURNALFOERT,     // Journaled
    FERDIGSTILT,      // Finalized
    EKSPEDERT,        // Sent/distributed
    UNDER_ARBEID,     // Being processed
    FEILREGISTRERT,   // Error state
    UTGAAR,           // Expired
    AVBRUTT,          // Cancelled
    UKJENT_BRUKER,    // Unknown person
    RESERVERT,        // Reserved
    OPPLASTING_DOKUMENT, // Uploading
    UKJENT            // Unknown
}
```

### DokumentInfo
```kotlin
data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String,
    val brevkode: String?,
    val dokumentvarianter: List<DokumentVariant>,
    val logiskeVedlegg: List<LogiskVedlegg>
)
```

### RelevantDato
```kotlin
data class RelevantDato(
    val dato: String,
    val datotype: Datotype
)

enum class Datotype {
    DATO_OPPRETTET,
    DATO_SENDT_PRINT,
    DATO_EKSPEDERT,
    DATO_JOURNALFOERT,
    DATO_REGISTRERT,
    DATO_AVS_RETUR,
    DATO_DOKUMENT
}
```

## Pagination

### Handling Large Result Sets
```kotlin
fun hentAlleDokumenter(saksnummer: String): List<Journalpost> {
    val result = mutableListOf<Journalpost>()
    var sluttpeker: String? = null

    do {
        val response = hentDokumentoversiktResponse(saksnummer, sluttpeker)
        result.addAll(response.journalposter)
        sluttpeker = if (response.sideInfo.finnesNesteSide) {
            response.sideInfo.sluttpeker
        } else null
    } while (sluttpeker != null)

    return result
}
```

## Access Control

### Checking Document Access
```kotlin
// Each DokumentVariant has saksbehandlerHarTilgang
val harTilgang = dokumentInfo.dokumentvarianter
    .any { it.saksbehandlerHarTilgang }

// If false, cannot download document content
```

### Tema-based Access
- SAF returns only documents matching user's tema access
- Melosys typically has access to: MED, TRY, UFM

## Error Handling

### GraphQL Errors
```kotlin
data class FeilResponseSafDto(
    val message: String?,
    val extensions: Extensions?
) {
    data class Extensions(
        val code: String?,
        val classification: String?
    )
}

// Check response for errors
if (response.errors?.isNotEmpty() == true) {
    val error = response.errors.first()
    when (error.extensions?.code) {
        "forbidden" -> throw TilgangException("No access to journalpost")
        "not_found" -> throw IkkeFunnetException("Journalpost not found")
        else -> throw TekniskException("SAF error: ${error.message}")
    }
}
```

## Usage in Melosys

### Validate Documents Belong to Case
```kotlin
// JoarkService.validerDokumenterTilhørerSakOgHarTilgang
fun validerDokumenterTilhørerSakOgHarTilgang(
    saksnummer: String,
    dokumentReferanser: List<DokumentReferanse>
) {
    val saksDokumenter = safConsumer.hentDokumentoversikt(saksnummer)
    dokumentReferanser.forEach { ref ->
        val finnes = saksDokumenter.any { jp ->
            jp.dokumenter.any { it.dokumentInfoId == ref.dokumentInfoId }
        }
        if (!finnes) {
            throw FunksjonellException("Dokument ${ref.dokumentInfoId} tilhører ikke sak")
        }
    }
}
```

### Get Document Reception Date
```kotlin
fun hentMottaksDatoForJournalpost(journalpostId: String): LocalDate? {
    val journalpost = safConsumer.hentJournalpost(journalpostId)
    return journalpost?.relevanteDatoer
        ?.find { it.datotype == Datotype.DATO_REGISTRERT }
        ?.dato?.let { LocalDate.parse(it) }
}
