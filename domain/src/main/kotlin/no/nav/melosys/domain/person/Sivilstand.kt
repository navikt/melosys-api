package no.nav.melosys.domain.person

import java.time.LocalDate

@JvmRecord
data class Sivilstand(
    val type: Sivilstandstype,
    val tekstHvisTypeErUdefinert: String?,
    val relatertVedSivilstand: String?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    val master: String?,
    val kilde: String?,
    val erHistorisk: Boolean
)
