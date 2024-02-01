package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.brev.bestilling.HentMuligeBrevmottakereService

@JvmRecord
data class HentMuligeBrevmottakereRequest(
    @JvmField val produserbartdokument: Produserbaredokumenter,
    @JvmField val orgnr: String,
    @JvmField val institusjonID: String
) {
    fun tilHentMuligeBrevmottakereRequestDto(behandlingID: Long?): HentMuligeBrevmottakereService.RequestDto {
        return HentMuligeBrevmottakereService.RequestDto(
            this.produserbartdokument,
            behandlingID!!,
            this.orgnr,
            this.institusjonID
        )
    }
}
