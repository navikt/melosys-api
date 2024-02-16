package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelse(val mottatteOpplysningerService: MottatteOpplysningerService) {
    fun hentVilkår(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long
    ): List<Vilkår> {
        return when (bestemmelse) {
            FTRL_KAP2_2_1_FØRSTE_LEDD -> ftrlKap2_1VilkårForBehandling(behandlingID)
            FTRL_KAP2_2_5_FØRSTE_LEDD_H -> listOf(Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER), Vilkår(FTRL_2_5_LÅN_STIPEND_LÅNEKASSEN))
            else -> emptyList()
        }
    }

    private fun ftrlKap2_1VilkårForBehandling(behandlingID: Long): List<Vilkår> {
        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)
        val vilkårForLand = ftrlKap2_1VilkårForLand(mottatteOpplysninger.mottatteOpplysningerData?.soeknadsland)

        return vilkårForLand + Vilkår(FTRL_2_1_LOVLIG_OPPHOLD)
    }

    private fun ftrlKap2_1VilkårForLand(søknadsland: Soeknadsland?): List<Vilkår> {
        if (søknadsland == null) {
            return emptyList()
        }

        // Ett eller flere land utenfor Norge. Vi legger alltid til grunn at det gjelder opphold i utlandet ved flere land.
        return if (søknadsland.landkoder.any { it != Land_iso2.NO.toString() } || søknadsland.isFlereLandUkjentHvilke) {
            listOf(
                Vilkår(FTRL_2_1_BOSATT_NORGE_FORUT),
                Vilkår(FTRL_2_1_OPPHOLD_UNDER_12MND)
            )
        } else {
            listOf(
                Vilkår(FTRL_2_1_BOSATT_NORGE)
            )
        }
    }

    data class Vilkår(
        val vilkår: Vilkaar,
        val muligeBegrunnelser: Collection<String> = emptyList(),
        val defaultOppfylt: Boolean? = null
    )
}
