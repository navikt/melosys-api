package no.nav.melosys.domain.person.adresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate

data class Bostedsadresse(
    override val strukturertAdresse: StrukturertAdresse?,
    override val coAdressenavn: String?,
    override val gyldigFraOgMed: LocalDate?,
    override val gyldigTilOgMed: LocalDate?,
    override val master: String?,
    override val kilde: String?,
    override val erHistorisk: Boolean
) : PersonAdresse {

    val hentStrukturertAdresse: StrukturertAdresse
        @JsonIgnore get() = strukturertAdresse!!

    @JsonIgnore
    override fun hentStrukturertAdresse(): StrukturertAdresse = strukturertAdresse!!

    @JsonIgnore
    override fun hentGyldigFraOgMed(): LocalDate = gyldigFraOgMed ?: error("gyldigFraOgMed er påkrevd for Bostedsadresse")

    @JsonIgnore
    override fun hentGyldigTilOgMed(): LocalDate = gyldigTilOgMed ?: error("gyldigTilOgMed er påkrevd for Bostedsadresse")

    override fun erGyldig(): Boolean {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig()
    }
}