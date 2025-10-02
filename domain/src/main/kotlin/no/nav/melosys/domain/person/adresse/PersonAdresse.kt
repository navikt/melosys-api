package no.nav.melosys.domain.person.adresse

import no.nav.melosys.domain.adresse.StrukturertAdresse
import java.time.LocalDate

interface PersonAdresse {
    val coAdressenavn: String?
    val strukturertAdresse: StrukturertAdresse?
    val gyldigFraOgMed: LocalDate?
    val gyldigTilOgMed: LocalDate?
    val master: String?
    val kilde: String?
    val erHistorisk: Boolean
    fun erGyldig(): Boolean

    fun hentStrukturertAdresse(): StrukturertAdresse = this.strukturertAdresse
        ?: error("strukturertAdresse mangler for denne instansen av ${this::class.simpleName}")

    fun hentGyldigFraOgMed(): LocalDate = this.gyldigFraOgMed
        ?: error("gyldigFraOgMed mangler for denne instansen av ${this::class.simpleName}")
}
