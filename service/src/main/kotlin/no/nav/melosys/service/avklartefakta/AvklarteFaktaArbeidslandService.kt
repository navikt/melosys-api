package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AvklarteFaktaArbeidslandService(private val avklartefaktaService: AvklartefaktaService) {

    @Transactional
    fun lagreArbeidslandSomAvklartefakta(behandlingID: Long, arbeidsland: List<String?>) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.ARBEIDSLAND)
        for (land in arbeidsland) {
            lagreArbeidslandSomAvklartfakta(land, behandlingID)
        }
    }

    fun lagreArbeidslandSomAvklartfakta(arbeidsland: String?, behandlingID: Long) {
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.ARBEIDSLAND,
            Avklartefaktatyper.ARBEIDSLAND.kode,
            arbeidsland,
            Avklartefakta.VALGT_FAKTA
        )
    }
}
