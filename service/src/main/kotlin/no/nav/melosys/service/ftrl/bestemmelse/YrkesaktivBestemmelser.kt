package no.nav.melosys.service.ftrl.bestemmelse

import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser.*

object YrkesaktivBestemmelser {
    val bestemmelser = listOf(
        FTRL_KAP2_2_1,
        FTRL_KAP2_2_2,
        FTRL_KAP2_2_3_ANDRE_LEDD,
        FTRL_KAP2_2_5_FØRSTE_LEDD_A,
        FTRL_KAP2_2_5_FØRSTE_LEDD_B,
        FTRL_KAP2_2_5_FØRSTE_LEDD_C,
        FTRL_KAP2_2_5_FØRSTE_LEDD_D,
        FTRL_KAP2_2_5_FØRSTE_LEDD_E,
        FTRL_KAP2_2_5_FØRSTE_LEDD_F,
        FTRL_KAP2_2_5_FØRSTE_LEDD_G,
        FTRL_KAP2_2_7_FØRSTE_LEDD,
        FTRL_KAP2_2_7A,
        FTRL_KAP2_2_8_FØRSTE_LEDD_A,
        FTRL_KAP2_2_8_ANDRE_LEDD,
    )

    val bestemmelserMedSpesielleGrupper: List<Bestemmelse> = listOf(
        FTRL_KAP2_2_1,
        FTRL_KAP2_2_2,
        FTRL_KAP2_2_3_ANDRE_LEDD,
        FTRL_KAP2_2_5_FØRSTE_LEDD_A,
        FTRL_KAP2_2_5_FØRSTE_LEDD_B,
        FTRL_KAP2_2_5_FØRSTE_LEDD_C,
        FTRL_KAP2_2_5_FØRSTE_LEDD_D,
        FTRL_KAP2_2_5_FØRSTE_LEDD_E,
        FTRL_KAP2_2_5_FØRSTE_LEDD_F,
        FTRL_KAP2_2_5_FØRSTE_LEDD_G,
        FTRL_KAP2_2_7_FØRSTE_LEDD,
        FTRL_KAP2_2_7A,
        FTRL_KAP2_2_8_FØRSTE_LEDD_A,
        FTRL_KAP2_2_8_FØRSTE_LEDD_B,
        FTRL_KAP2_2_8_ANDRE_LEDD,
        ARKTISK_RÅDS_SEKRETARIAT_ART16,
        DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14,
        DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16,
        TILLEGGSAVTALE_NATO,
    )
}
