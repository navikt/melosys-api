package no.nav.melosys.domain.person.adresse

import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate

@JvmRecord
data class Bostedsadresse(
    override val strukturertAdresse: StrukturertAdresse?,
    override val coAdressenavn: String?,
    override val gyldigFraOgMed: LocalDate?,
    override val gyldigTilOgMed: LocalDate?,
    override val master: String?,
    override val kilde: String?,
    override val erHistorisk: Boolean
) : PersonAdresse {

    override fun erGyldig(): Boolean {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig()
    }
}
