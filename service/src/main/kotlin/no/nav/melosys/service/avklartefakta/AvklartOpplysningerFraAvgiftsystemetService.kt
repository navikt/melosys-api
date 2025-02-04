package no.nav.melosys.service.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper.OPPLYSNINGER_FRA_AVGIFTSYSTEMET
import org.springframework.stereotype.Service

@Service
class AvklartOpplysningerFraAvgiftsystemetService(private val avklartefaktaService: AvklartefaktaService) {

    fun hentOpplysningerFraAvgiftsystemet(behandlingID: Long): Boolean? {
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID)
            .filter { OPPLYSNINGER_FRA_AVGIFTSYSTEMET.kode == it.referanse && OPPLYSNINGER_FRA_AVGIFTSYSTEMET == it.avklartefaktaType }
            .map { it.fakta.single().toBoolean() }
            .firstOrNull()
    }

    fun lagreOpplysningerFraAvgiftsystemetSomAvklartFakta(behandlingID: Long, fullstendigManglendeInnbetaling: Boolean) {
        avklartefaktaService.slettAvklarteFakta(behandlingID, OPPLYSNINGER_FRA_AVGIFTSYSTEMET)

        avklartefaktaService.leggTilAvklarteFakta(
            behandlingID, OPPLYSNINGER_FRA_AVGIFTSYSTEMET, OPPLYSNINGER_FRA_AVGIFTSYSTEMET.kode,
            null, fullstendigManglendeInnbetaling.toString().uppercase()
        )
    }
}
