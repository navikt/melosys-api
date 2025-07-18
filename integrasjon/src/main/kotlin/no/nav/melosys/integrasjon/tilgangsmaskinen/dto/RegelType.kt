package no.nav.melosys.integrasjon.tilgangsmaskinen.dto

/**
 * Regeltyper støttet av Tilgangsmaskinen API
 * Basert på offisiell OpenAPI spesifikasjon.
 */
enum class RegelType {
    /**
     * Grunnleggende tilgangsregler - anbefalt for de fleste tilfeller
     */
    KJERNE_REGELTYPE,

    /**
     * Alle tilgangsregler inkludert utvidede kontroller
     */
    KOMPLETT_REGELTYPE,

    /**
     * Regler som kan overstyres av saksbehandler
     */
    OVERSTYRBAR_REGELTYPE
}
