package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype
import org.springframework.stereotype.Service

@Service
class AvklartArbeidssituasjonTypeService(private val avklartefaktaService: AvklartefaktaService) {
    fun lagreArbeidssituasjonTypeSomAvklarteFakta(behandlingID: Long, arbeidssituasjontype: Arbeidssituasjontype) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.ARBEIDSSITUASJON)
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.ARBEIDSSITUASJON,
            Avklartefaktatyper.ARBEIDSSITUASJON.kode,
            null,
            arbeidssituasjontype.kode
        )
    }
}
