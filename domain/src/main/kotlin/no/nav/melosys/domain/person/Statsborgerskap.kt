package no.nav.melosys.domain.person

import java.time.LocalDate

@JvmRecord
data class Statsborgerskap(
    val landkode: String?,
    val bekreftelsesdato: LocalDate?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val master: String?,
    val kilde: String?,
    val erHistorisk: Boolean
) {

    fun erBekreftetPåDato(dato: LocalDate): Boolean =
        bekreftelsesdato != null && !bekreftelsesdato.isAfter(dato)

    fun erGyldigPåDato(dato: LocalDate): Boolean =
        erGyldigFraOgMedDato(dato) && erGyldigTilOgMedDato(dato)

    private fun erGyldigFraOgMedDato(dato: LocalDate): Boolean =
        (gyldigFraOgMed == null && !erHistorisk) || (gyldigFraOgMed != null && !gyldigFraOgMed.isAfter(dato))

    private fun erGyldigTilOgMedDato(dato: LocalDate): Boolean =
        gyldigTilOgMed == null || gyldigTilOgMed.isAfter(dato)
}