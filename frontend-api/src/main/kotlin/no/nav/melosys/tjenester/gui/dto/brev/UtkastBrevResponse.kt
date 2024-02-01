package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.brev.utkast.UtkastBrev

@JvmRecord
data class UtkastBrevResponse(
    val utkastBrevID: Long,
    val lagretAvSaksbehandlerIdent: String,
    val tittel: String,  // Vi ønsker lik data som både respons og request, derav bruker vi BrevbestillingRequest også i responsen
    val brevbestilling: BrevbestillingRequest
) {
    companion object {
        @JvmStatic
        fun av(utkastBrev: UtkastBrev): UtkastBrevResponse {
            return UtkastBrevResponse(
                utkastBrev.id,
                utkastBrev.lagretAvSaksbehandler,
                utkastBrev.brevbestillingUtkast.tittel,
                BrevbestillingRequest.av(utkastBrev.brevbestillingUtkast)
            )
        }
    }
}
