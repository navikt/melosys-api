package no.nav.melosys.domain.person.adresse

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
data class Oppholdsadresse(
    @JsonProperty("strukturertAdresse") override val strukturertAdresse: StrukturertAdresse?,
    @JsonProperty("coAdressenavn") override val coAdressenavn: String?,
    @JsonProperty("gyldigFraOgMed") override val gyldigFraOgMed: LocalDate?,
    @JsonProperty("gyldigTilOgMed") override val gyldigTilOgMed: LocalDate?,
    @JsonProperty("master") override val master: String?,
    @JsonProperty("kilde") override val kilde: String?,
    val registrertDato: LocalDateTime?,
    @JsonProperty("erHistorisk") override val erHistorisk: Boolean
) : PersonAdresse {

    override fun erGyldig(): Boolean =
        !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig()
}