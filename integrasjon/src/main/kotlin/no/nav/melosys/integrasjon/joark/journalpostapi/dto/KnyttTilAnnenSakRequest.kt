package no.nav.melosys.integrasjon.joark.journalpostapi.dto

/**
 * Payload til Joark-tjenesten
 * PUT /rest/journalpostapi/v1/journalpost/{kildeJournalpostId}/knyttTilAnnenSak.
 *
 * Joark utfører selv tilgangssjekk, validering og kopiering av journalposten.
 */
data class KnyttTilAnnenSakRequest(
    val sakstype: Sakstype,
    val fagsakId: String?,
    val fagsaksystem: String?,
    val tema: String,
    val bruker: Bruker,
    val journalfoerendeEnhet: String,
    val dokumenter: List<Long>? = null
) {
    enum class Sakstype {
        FAGSAK,
        GENERELL_SAK
    }
}
