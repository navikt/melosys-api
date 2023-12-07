package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AvklartManglendeInnbetalingService(@Autowired private val avklartefaktaService: AvklartefaktaService) {

    fun lagreFullstendigManglendeInnbetalingSomAvklartFakta(behandlingID: Long, fullstendigManglendeInnbetaling: Boolean) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING)

        avklartefaktaService.leggTilAvklarteFakta(behandlingID, FULLSTENDIG_MANGLENDE_INNBETALING, FULLSTENDIG_MANGLENDE_INNBETALING.kode,
            null, fullstendigManglendeInnbetaling.toString().uppercase())
    }
}
