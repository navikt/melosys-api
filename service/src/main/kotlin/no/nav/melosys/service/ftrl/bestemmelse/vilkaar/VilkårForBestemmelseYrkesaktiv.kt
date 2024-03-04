package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelseYrkesaktiv(val mottatteOpplysningerService: MottatteOpplysningerService) {
    fun hentVilkår(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> {
        return when (bestemmelse) {
            FTRL_KAP2_2_2,
            FTRL_KAP2_2_3_ANDRE_LEDD,
            FTRL_KAP2_2_5_FØRSTE_LEDD_A,
            FTRL_KAP2_2_5_FØRSTE_LEDD_B,
            FTRL_KAP2_2_5_FØRSTE_LEDD_C,
            FTRL_KAP2_2_5_FØRSTE_LEDD_D,
            FTRL_KAP2_2_5_FØRSTE_LEDD_E,
            FTRL_KAP2_2_5_FØRSTE_LEDD_F,
            FTRL_KAP2_2_5_FØRSTE_LEDD_G -> emptyList()

            FTRL_KAP2_2_7_FØRSTE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_7_IKKE_PLIKTIG_MEDLEM),
                Vilkår(FTRL_2_7_RIMELIGHETSVURDERING, muligeBegrunnelser = toStringList(*Ftrl_2_7_begrunnelser.values()))
            )

            FTRL_KAP2_2_7A -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_7A_BOSATT_I_NORGE),
                Vilkår(FTRL_2_7A_SKIP_UTENFOR_EØS)
            )

            FTRL_KAP2_2_8_FØRSTE_LEDD_A-> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_8_FORUTGÅENDE_TRYGDETID),
                Vilkår(FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE)
            )

            FTRL_KAP2_2_8_ANDRE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_8_FORUTGÅENDE_TRYGDETID),
                Vilkår(
                    FTRL_2_8_NÆR_TILKNYTNING_NORGE,
                    muligeBegrunnelser = toStringList(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values())
                )
            )

            else -> emptyList()
        }
    }


    companion object {
        internal fun toStringList(vararg kodeverkVerdier: Kodeverk): Collection<String> = kodeverkVerdier.map { it.kode }.toList()
    }
}
