package no.nav.melosys.domain.mottatteopplysninger.data

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.adresse.StrukturertAdresse


/**
 * Opplysninger om foretak i utlandet
 */
class ForetakUtland {
    // Settes av frontend eller ved mapping fra SED for hvert foretak fordi orgnr ikke er påkrevd,
    // og defor ikke kan brukes som nøkkel
    var uuid: String? = null
    var navn: String? = null
    var orgnr: String? = null
    var adresse = StrukturertAdresse()

    @JsonProperty("selvstendigNaeringsvirksomhet")
    var selvstendigNæringsvirksomhet: Boolean? = null
}
