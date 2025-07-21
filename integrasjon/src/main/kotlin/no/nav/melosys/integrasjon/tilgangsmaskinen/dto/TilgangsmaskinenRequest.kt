package no.nav.melosys.integrasjon.tilgangsmaskinen.dto

/**
 * Request for Tilgangsmaskinen API
 *
 * For enkelt-bruker endepunkter sendes bare fnr/dnr som en string direkte.
 * Ingen roller spesifiseres - disse evalueres automatisk av Tilgangsmaskinen.
 *
 * @param brukerIdent Fødselsnummer, d-nummer eller NPID (11 tegn)
 */
data class TilgangsmaskinenRequest(
    val brukerIdent: String
)
