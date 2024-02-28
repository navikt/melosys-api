package no.nav.melosys.service.dokument.brev

import no.nav.melosys.sikkerhet.context.SubjectHandler

open class BrevData(
    var saksbehandler: String? = null,
    var fritekst: String? = null,
    var begrunnelseKode: String? = null
) {
    constructor(brevbestillingDto: BrevbestillingDto) : this(
        SubjectHandler.getInstance().getUserID(),
        brevbestillingDto.fritekst,
        brevbestillingDto.begrunnelseKode
    )

    constructor(brevbestillingDto: BrevbestillingDto, saksbehandler: String?) : this(
        saksbehandler,
        brevbestillingDto.fritekst,
        brevbestillingDto.begrunnelseKode
    )
}


