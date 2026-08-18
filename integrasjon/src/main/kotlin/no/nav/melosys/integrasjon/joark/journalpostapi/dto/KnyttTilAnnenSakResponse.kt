package no.nav.melosys.integrasjon.joark.journalpostapi.dto

/**
 * Svar fra Joark-tjenesten knyttTilAnnenSak. Inneholder ID-en til den nye journalposten
 * som dokumentene ble kopiert over til.
 */
data class KnyttTilAnnenSakResponse(
    val nyJournalpostId: String
)
