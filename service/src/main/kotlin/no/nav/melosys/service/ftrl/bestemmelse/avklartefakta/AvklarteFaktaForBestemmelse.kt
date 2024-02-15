package no.nav.melosys.service.ftrl.bestemmelse.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class AvklarteFaktaForBestemmelse(val mottatteOpplysningerService: MottatteOpplysningerService) {
    fun hentAvklarteFakta(bestemmelse: Folketrygdloven_kap2_bestemmelser, behandlingID: Long): List<AvklarteFaktaType> {
        return when (bestemmelse) {
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD -> ftrlKap2_1AvklarteFaktaForBehandling(behandlingID)
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD -> listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                        Ikkeyrkesaktivrelasjontype.BARN_2_5_ANDRE_LEDD,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_C_TIL_E,
                    ).map(Ikkeyrkesaktivrelasjontype::name)
                )
            )
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD -> listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                        Ikkeyrkesaktivrelasjontype.BARN_2_8_FJERDE_LEDD,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD,
                    ).map(Ikkeyrkesaktivrelasjontype::name)
                )
            )
            else -> emptyList()
        }
    }

    private fun ftrlKap2_1AvklarteFaktaForBehandling(behandlingID: Long): List<AvklarteFaktaType> {
        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)

        return ftrlKap2_1AvklarteFaktaForLand(mottatteOpplysninger.mottatteOpplysningerData?.soeknadsland)
    }

    private fun ftrlKap2_1AvklarteFaktaForLand(søknadsland: Soeknadsland?): List<AvklarteFaktaType> {
        if (søknadsland == null) {
            return emptyList()
        }

        // Ett eller flere land utenfor Norge. Vi legger alltid til grunn at det gjelder opphold i utlandet ved flere land.
        if (søknadsland.landkoder.filter { s -> s != Landkoder.NO.toString() }.isNotEmpty() || søknadsland.isErUkjenteEllerAlleEosLand) {
            return listOf(AvklarteFaktaType(Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD, emptyList()))
        }
        return emptyList()
    }

    data class AvklarteFaktaType(val type: Avklartefaktatyper, val muligeFakta: List<String>)
}
