package no.nav.melosys.tjenester.gui.dto

@JvmRecord
data class StrukturertAdresseDto(
    val gatenavn: String?,
    val husnummer: String?,
    val postnummer: String?,
    val poststed: String?,
    val region: String?,
    val landkode: String?
)
