package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Betalingstype
import org.springframework.stereotype.Component

@Component
class AvklarteBetalingsvalgService(private val avklartefaktaService: AvklartefaktaService) {

    fun lagreBetalingsvalgSomAvklartefakta(behandlingID: Long, betalingstype: Betalingstype) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.BETALINGSVALG)
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.BETALINGSVALG,
            Avklartefaktatyper.BETALINGSVALG.kode,
            null,
            betalingstype.kode,
        )
    }

    fun hentAvklarteBetalingsvalg(behandlingID: Long): Betalingstype? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
                .filter { it.avklartefaktaType == Avklartefaktatyper.BETALINGSVALG}
                .map { Betalingstype.valueOf(it.fakta.single()) }
                .firstOrNull()
    }
}
