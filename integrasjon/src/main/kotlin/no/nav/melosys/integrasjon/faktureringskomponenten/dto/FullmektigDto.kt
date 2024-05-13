package no.nav.melosys.integrasjon.faktureringskomponenten.dto

import no.nav.melosys.domain.Aktoer

data class FullmektigDto(
    val fodselsnummer: String?,
    val organisasjonsnummer: String?,
) {
    constructor(aktoer: Aktoer?) : this(fodselsnummer = aktoer?.personIdent, organisasjonsnummer = aktoer?.orgnr)
}
