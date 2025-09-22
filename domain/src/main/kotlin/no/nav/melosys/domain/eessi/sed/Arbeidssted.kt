package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import org.apache.commons.lang3.StringUtils

class Arbeidssted {
    var navn: String? = null
        set(value) {
            field = if (StringUtils.isBlank(value)) IKKE_TILGJENGELIG else value
        }

    var adresse: Adresse? = null

    var fysisk: Boolean = false

    var hjemmebase: String? = null

    fun tilFysiskArbeidssted(): FysiskArbeidssted =
        FysiskArbeidssted(navn, adresse?.tilStrukturertAdresse() ?: throw IllegalStateException("Adresse cannot be null"))

    companion object {
        private const val IKKE_TILGJENGELIG = "N/A"

        @JvmStatic
        fun lagIkkeFastArbeidssted(landkode: String) = Arbeidssted().apply {
            navn = Adresse.INGEN_FAST_ADRESSE
            adresse = Adresse.lagIkkeFastAdresse(landkode)
        }
    }
}
