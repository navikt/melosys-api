package no.nav.melosys.tjenester.gui.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.adresse.UstrukturertAdresse

@JsonInclude(JsonInclude.Include.NON_NULL)
class MidlertidigPostadresseDto {
    enum class Adressetype {
        STRUKTURERT, USTRUKTURERT
    }

    @JvmField
    var adressetype: Adressetype? = null

    @JvmField
    var strukturertAdresse: StrukturertAdresseDto? = null

    @JvmField
    var ustrukturertAdresse: UstrukturertAdresse? = null
}
