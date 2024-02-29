package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.Utpekingsperiode

class BrevDataUtpekingAnnetLand(
    brevbestillingDto: BrevbestillingDto,
    saksbehandler: String
) : BrevData(brevbestillingDto, saksbehandler) {
    var utpekingsperiode: Utpekingsperiode? = null
}
