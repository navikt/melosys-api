package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker
import no.nav.melosys.service.brev.bestilling.HentMuligeBrevmottakereService.ResponseDto

@JvmRecord
data class HentMuligeBrevmottakereResponse(
    val hovedMottaker: MuligBrevmottaker,
    val kopiMottakere: List<MuligBrevmottaker>,
    val fasteMottakere: List<MuligBrevmottaker>
) {
    companion object {
        @JvmStatic
        fun av(hentMottakerResponse: ResponseDto): HentMuligeBrevmottakereResponse {
            val hovedMottaker = MuligBrevmottaker.av(hentMottakerResponse.hovedMottaker)
            val kopiMottakere = hentMottakerResponse.kopiMottakere.stream().map { hovedMottaker: Brevmottaker? ->
                MuligBrevmottaker.av(
                    hovedMottaker!!
                )
            }.toList()
            val fasteMottakere = hentMottakerResponse.fasteMottakere.stream().map { hovedMottaker: Brevmottaker? ->
                MuligBrevmottaker.av(
                    hovedMottaker!!
                )
            }.toList()
            return HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere)
        }
    }
}
