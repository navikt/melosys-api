package no.nav.melosys.domain.arkiv

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * JournalpostID + dokumentID identifiserer et dokument i arkivet.
 */
data class DokumentReferanse @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("journalpostID") val journalpostID: String,
    @JsonProperty("dokumentID") val dokumentID: String
)
