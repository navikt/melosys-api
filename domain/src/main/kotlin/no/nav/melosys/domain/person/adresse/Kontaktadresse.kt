package no.nav.melosys.domain.person.adresse

import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
data class Kontaktadresse(
    override val strukturertAdresse: StrukturertAdresse?,
    val semistrukturertAdresse: SemistrukturertAdresse?,
    override val coAdressenavn: String?,
    override val gyldigFraOgMed: LocalDate?,
    override val gyldigTilOgMed: LocalDate?,
    override val master: String?,
    override val kilde: String?,
    val registrertDato: LocalDateTime?,
    override val erHistorisk: Boolean
) : PersonAdresse {

    override fun erGyldig(): Boolean {
        val adresse = hentEllerLagStrukturertAdresse()
        return !erHistorisk && adresse != null && adresse.erGyldig()
    }

    fun hentEllerLagStrukturertAdresse(): StrukturertAdresse? =
        strukturertAdresse ?: semistrukturertAdresse?.tilStrukturertAdresse()

    fun hentRegistrertDato(): LocalDateTime =
        registrertDato ?: error("registrertDato er påkrevd for ${this::class.simpleName}")

}
