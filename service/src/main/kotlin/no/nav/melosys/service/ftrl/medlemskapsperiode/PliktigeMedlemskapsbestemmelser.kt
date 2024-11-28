package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser

object PliktigeMedlemskapsbestemmelser {
    val bestemmelser = listOf(
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_B,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_C,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_D,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_F,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_G,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD
    )

    val bestemmelserMedSpesielleGrupper: List<Bestemmelse> = listOf(
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_B,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_C,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_D,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_F,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_G,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD,
        Vertslandsavtale_bestemmelser.ARKTISK_RÅDS_SEKRETARIAT_ART16,
        Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14,
        Vertslandsavtale_bestemmelser.DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16,
        Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO,
    )
}
