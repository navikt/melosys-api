package no.nav.melosys.integrasjon.dokgen.dto

import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller

class ÅrsavregningVedtaksbrev(
    brevBestilling: ÅrsavregningVedtakBrevBestilling,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
) : DokgenDto(brevBestilling, Mottakerroller.BRUKER) {
    constructor(
        brevBestilling: ÅrsavregningVedtakBrevBestilling,
    ) : this(
        brevBestilling = brevBestilling,
        innledningFritekst = brevBestilling.innledningFritekstAarsavregning,
        begrunnelseFritekst = brevBestilling.begrunnelseFritekstAarsavregning
    )
}
