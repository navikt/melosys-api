package no.nav.melosys.tjenester.gui.dto

@JvmRecord
data class LagreFritekstDto(
    @JvmField val innledningFritekst: String,
    @JvmField val begrunnelseFritekst: String,
    @JvmField val trygdeavgiftFritekst: String
)
