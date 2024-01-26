package no.nav.melosys.tjenester.gui.dto.utpeking

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis

class UtpekingAvvisDto {
    var fritekst: String? = null
    var nyttLovvalgsland: String? = null
    var begrunnelseUtenlandskMyndighet: String? = null
    var isVilSendeAnmodningOmMerInformasjon: Boolean? = null

    fun tilDomene(): UtpekingAvvis {
        return UtpekingAvvis(
            begrunnelseUtenlandskMyndighet,
            isVilSendeAnmodningOmMerInformasjon!!,
            nyttLovvalgsland,
            fritekst
        )
    }
}
