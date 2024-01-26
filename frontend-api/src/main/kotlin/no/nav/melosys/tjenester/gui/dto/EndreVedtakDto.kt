package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode

class EndreVedtakDto {
    @JvmField
    var begrunnelseKode: Endretperiode? = null
    @JvmField
    var fritekst: String? = null
    @JvmField
    var fritekstSed: String? = null
}
