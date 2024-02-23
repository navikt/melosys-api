package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivoppholdtype
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AvklartOppholdTypeService(@Autowired private val avklartefaktaService: AvklartefaktaService) {
    fun lagreOppholdstypeSomAvklarteFakta(behandlingID: Long, ikkeyrkesaktivoppholdtype: Ikkeyrkesaktivoppholdtype) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD)
        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID,
            Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD,
            Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD.kode,
            null,
            ikkeyrkesaktivoppholdtype.kode
        )
    }
}
