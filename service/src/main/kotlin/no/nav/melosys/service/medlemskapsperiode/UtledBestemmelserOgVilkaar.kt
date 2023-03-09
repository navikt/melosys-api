package no.nav.melosys.service.medlemskapsperiode;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

class UtledBestemmelserOgVilkaar {
    val støttetBestemmelserOgVilkaar = listOf(FTRL_KAP2_2_8_FØRSTE_LEDD_A, FTRL_KAP2_2_8_ANDRE_LEDD)

    val yrkesaktivBestemmelser = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_2, setOf()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_3_ANDRE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_5, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_A, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_B, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_C, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_D, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_E, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_F, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_G, setOf()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_A, setOf()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_B, setOf()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_C, setOf()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_7A, setOf()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_A, setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID)),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        ),
        Pair(FTRL_KAP2_2_8_TREDJE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FEMTE_LEDD, setOf()),
    )

    val ikkeYrkesaktivBestemmelser = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_5, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_C, setOf()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_H, setOf()),
        Pair(FTRL_KAP2_2_5_ANDRE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_7_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_B, setOf()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_C, setOf()),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        ),
        Pair(FTRL_KAP2_2_8_TREDJE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FEMTE_LEDD, setOf()),
    )

    val pensjonistBestemmelser = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_5, setOf()),
        Pair(FTRL_KAP2_2_5_ANDRE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_7_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_D, setOf()),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        ),
        Pair(FTRL_KAP2_2_8_TREDJE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FJERDE_LEDD, setOf()),
        Pair(FTRL_KAP2_2_8_FEMTE_LEDD, setOf()),
    )

    val defaultBestemmelser = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_A, setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID)),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        ),

        )

    private fun bestemmelseOgVilkaarFraBehandlingstema(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        when (behandlingstema) {
            Behandlingstema.YRKESAKTIV -> yrkesaktivBestemmelser
            Behandlingstema.IKKE_YRKESAKTIV -> ikkeYrkesaktivBestemmelser
            Behandlingstema.PENSJONIST -> pensjonistBestemmelser
            else -> defaultBestemmelser
        }

    fun hentStøttedeBestemmelserOgVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        bestemmelseOgVilkaarFraBehandlingstema(behandlingstema).filter { støttetBestemmelserOgVilkaar.contains(it.key) }

    fun hentIkkeStøttedeBestemmelserOgVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        bestemmelseOgVilkaarFraBehandlingstema(behandlingstema).filter { !støttetBestemmelserOgVilkaar.contains(it.key) }
}
