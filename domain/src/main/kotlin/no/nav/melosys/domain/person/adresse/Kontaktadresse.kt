package no.nav.melosys.domain.person.adresse

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
data class Kontaktadresse(
    @JsonProperty("strukturertAdresse") override val strukturertAdresse: StrukturertAdresse?,
    val semistrukturertAdresse: SemistrukturertAdresse?,
    @JsonProperty("coAdressenavn") override val coAdressenavn: String?,
    @JsonProperty("gyldigFraOgMed") override val gyldigFraOgMed: LocalDate?,
    @JsonProperty("gyldigTilOgMed") override val gyldigTilOgMed: LocalDate?,
    @JsonProperty("master") override val master: String?,
    @JsonProperty("kilde") override val kilde: String?,
    val registrertDato: LocalDateTime?,
    @JsonProperty("erHistorisk") override val erHistorisk: Boolean
) : PersonAdresse {

    override fun erGyldig(): Boolean {
        val adresse = hentEllerLagStrukturertAdresse()
        return !erHistorisk && adresse != null && adresse.erGyldig()
    }

    fun hentEllerLagStrukturertAdresse(): StrukturertAdresse? =
        strukturertAdresse ?: semistrukturertAdresse?.tilStrukturertAdresse()

}