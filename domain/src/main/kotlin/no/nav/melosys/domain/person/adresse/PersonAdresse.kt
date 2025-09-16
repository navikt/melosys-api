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

    // Non-null accessors with error messages for required fields
    fun hentStrukturertAdresse(): StrukturertAdresse {
        val strukturertAdresse = strukturertAdresse
        return strukturertAdresse
            ?: error("strukturertAdresse er påkrevd for ${this::class.simpleName}")
    }

    fun hentGyldigFraOgMed(): LocalDate {
        val gyldigFraOgMed = gyldigFraOgMed
        return gyldigFraOgMed
            ?: error("gyldigFraOgMed er påkrevd for ${this::class.simpleName}")
    }

    fun hentGyldigTilOgMed(): LocalDate {
        val gyldigTilOgMed = gyldigTilOgMed
        return gyldigTilOgMed
            ?: error("gyldigTilOgMed er påkrevd for ${this::class.simpleName}")
    }
}
