package no.nav.melosys.tjenester.gui.dto

@JvmRecord
data class FagsakSokDto(
    @JvmField val ident: String?,
    @JvmField val saksnummer: String?,
    @JvmField val orgnr: String?
)
