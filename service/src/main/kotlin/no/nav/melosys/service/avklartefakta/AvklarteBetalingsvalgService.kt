package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Betalingstype
import org.springframework.stereotype.Service

@Service
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

    fun hentBetalingsvalg(behandlingID: Long): Boolean? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { Avklartefaktatyper.BETALINGSVALG.kode == it.referanse && Avklartefaktatyper.BETALINGSVALG == it.avklartefaktaType }
            .map { it.fakta.single().toBoolean() }
            .firstOrNull()
    }

}
