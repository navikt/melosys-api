package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.service.brev.bestilling.HentMuligeBrevmottakereService.ResponseDto

data class HentMuligeBrevmottakereResponse(
    val hovedMottaker: MuligBrevmottaker?,
    val kopiMottakere: List<MuligBrevmottaker> = emptyList(),
    val fasteMottakere: List<MuligBrevmottaker> = emptyList()
) {
    companion object {
        fun av(hentMottakerResponse: ResponseDto) = HentMuligeBrevmottakereResponse(
            hovedMottaker = MuligBrevmottaker.av(hentMottakerResponse.hovedMottaker),
            kopiMottakere = hentMottakerResponse.kopiMottakere?.mapNotNull { MuligBrevmottaker.av(it) } ?: emptyList(),
            fasteMottakere = hentMottakerResponse.fasteMottakere?.mapNotNull { MuligBrevmottaker.av(it) } ?: emptyList()
        )
    }
}
