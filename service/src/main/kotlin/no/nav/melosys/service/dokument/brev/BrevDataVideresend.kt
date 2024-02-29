package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.UtenlandskMyndighet

class BrevDataVideresend(
    brevbestillingDto: BrevbestillingDto,
    saksbehandler: String
) : BrevData(brevbestillingDto, saksbehandler) {
    var bostedsland: String? = null
    var trygdemyndighet: UtenlandskMyndighet? = null
}
