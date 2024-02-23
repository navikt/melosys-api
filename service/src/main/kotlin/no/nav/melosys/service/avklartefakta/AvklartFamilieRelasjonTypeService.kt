package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AvklartFamilieRelasjonTypeService(@Autowired private val avklartefaktaService: AvklartefaktaService) {
    fun lagreFamilierelasjonstypeSomAvklarteFakta(behandlingID: Long, ikkeyrkesaktivrelasjontype: Ikkeyrkesaktivrelasjontype) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON)
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON,
            Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON.kode,
            null,
            ikkeyrkesaktivrelasjontype.kode
        )
    }
}
