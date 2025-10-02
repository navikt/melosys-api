package no.nav.melosys.domain.person.adresse

import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
data class Oppholdsadresse(
    override val strukturertAdresse: StrukturertAdresse?,
    override val coAdressenavn: String?,
    override val gyldigFraOgMed: LocalDate?,
    override val gyldigTilOgMed: LocalDate?,
    override val master: String?,
    override val kilde: String?,
    val registrertDato: LocalDateTime?,
    override val erHistorisk: Boolean
) : PersonAdresse {

    override fun erGyldig(): Boolean =
        !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig()

    fun hentRegistrertDato(): LocalDateTime =
        registrertDato ?: error("registrertDato er påkrevd for ${this::class.simpleName}")

}
