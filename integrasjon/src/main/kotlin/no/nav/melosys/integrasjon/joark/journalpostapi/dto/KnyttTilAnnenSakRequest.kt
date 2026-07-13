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
    /** Dersom man ikke oppgir noen dokumenter, kopieres hele journalposten over. */
    val dokumenter: List<Long>? = null
) {
    /** Ved automatisk journalføring uten mennesker involvert skal enhet settes til "9999". */
    val journalfoerendeEnhet: String = AUTOMATISK_JOURNALFOERENDE_ENHET

    enum class Sakstype {
        FAGSAK,
        GENERELL_SAK
    }

    private companion object {
        const val AUTOMATISK_JOURNALFOERENDE_ENHET = "9999"
    }
}
