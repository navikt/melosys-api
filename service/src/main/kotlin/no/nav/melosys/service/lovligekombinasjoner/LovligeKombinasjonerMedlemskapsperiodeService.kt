package no.nav.melosys.service.lovligekombinasjoner

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import org.springframework.stereotype.Service

@Service
class LovligeKombinasjonerMedlemskapsperiodeService() {

    val pliktigeBestemmelser = listOf(
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
        Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FJERDE_LEDD,
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

    val lovligeKombinasjonerDekningBestemmelse = mapOf(
        listOf(Trygdedekninger.FULL_DEKNING_FTRL) to listOf(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A,
            *pliktigeBestemmelser.toTypedArray()
        ),

        listOf(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER) to listOf(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD,
        ),

        listOf(Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER) to listOf(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
        ),

        listOf(
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        ) to listOf(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD
        )
    )

    fun hentLovligeBestemmelser(trygdedekning: Trygdedekninger): List<Folketrygdloven_kap2_bestemmelser> {
        return lovligeKombinasjonerDekningBestemmelse[lovligeKombinasjonerDekningBestemmelse.keys.find { it.contains(trygdedekning) }] ?: emptyList()
    }

    fun hentLovligeTrygdedekninger(bestemmelse: Folketrygdloven_kap2_bestemmelser): List<Trygdedekninger> {
        return lovligeKombinasjonerDekningBestemmelse.filterValues { it.contains(bestemmelse) }.keys.flatten()
    }

    fun erGyldigKombinasjon(bestemmelse: Folketrygdloven_kap2_bestemmelser, trygdedekning: Trygdedekninger): Boolean {
        return bestemmelse in hentLovligeBestemmelser(trygdedekning)
    }
}
