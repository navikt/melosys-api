package no.nav.melosys.domain.person

import java.time.LocalDate
import no.nav.melosys.domain.kodeverk.Personstatuser

@JvmRecord
data class Folkeregisterpersonstatus(
    val personstatus: Personstatuser,
    val tekstHvisStatusErUdefinert: String?,
    val master: String,
    val kilde: String,
    val fregGyldighetstidspunkt: LocalDate?,
    val erHistorisk: Boolean
) {
    fun hentGjeldendeTekst(): String =
        if (personstatus == Personstatuser.UDEFINERT)
            tekstHvisStatusErUdefinert ?: error("tekstHvisStatusErUdefinert er påkrevd når personstatus er UDEFINERT")
        else
            personstatus.getBeskrivelse()
}