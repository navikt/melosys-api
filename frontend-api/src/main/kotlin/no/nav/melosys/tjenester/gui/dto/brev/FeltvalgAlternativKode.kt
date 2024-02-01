package no.nav.melosys.tjenester.gui.dto.brev

enum class FeltvalgAlternativKode(@JvmField val beskrivelse: String) {
    HENVENDELSE_OM_TRYGDETILHØRLIGHET("Svar på henvendelse om trygdetilhørlighet"),
    ORIENTERING_BESLUTNING("Orientering om vår beslutning"),
    CONFIRMATION_OF_MEMBERSHIP("Confirmation of membership in the National Insurance Scheme"),
    BEKREFTELSE_PÅ_MEDLEMSKAP("Bekreftelse på medlemskap i folketrygden"),
    HENVENDELSE_OM_MEDLEMSKAP("Svar på henvendelse om medlemskap i folketrygden"),
    FRITEKST("Fritekst"),
    ENGELSK_FRITEKSTBREV("Request to remain subject to Norwegian legislation"),
    STANDARD("Standardtekst søknad/klage");

    val kode: String
        get() = name
}
