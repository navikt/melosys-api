package no.nav.melosys.tjenester.gui.dto

import java.time.LocalDate

class OrganisasjonDto {
    @JvmField
    var orgnr: String? = null
    @JvmField
    var navn: String? = null
    @JvmField
    var oppstartdato: LocalDate? = null
    @JvmField
    var organisasjonsform: String? = null
    @JvmField
    var forretningsadresse: AdresseDto? = null
    @JvmField
    var postadresse: AdresseDto? = null
}
