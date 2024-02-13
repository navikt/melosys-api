package no.nav.melosys.service.dokument.brev

class BrevDataVedlegg(
    saksbehandler: String
) : BrevData(saksbehandler = saksbehandler) {
    var brevDataA1: BrevDataA1? = null
    var brevDataA001: BrevDataA001? = null
}
