package no.nav.melosys.service.dokument.brev

open class BrevData(
    var saksbehandler: String? = null,
    var fritekst: String? = null,
    var begrunnelseKode: String? = null
) {

    constructor(brevbestillingDto: BrevbestillingDto, saksbehandler: String?) : this(
        saksbehandler,
        brevbestillingDto.fritekst,
        brevbestillingDto.begrunnelseKode
    )
}


