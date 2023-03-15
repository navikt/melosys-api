package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

class UtledBestemmelserOgVilkår {
    val støttetBestemmelser = listOf(FTRL_KAP2_2_8_FØRSTE_LEDD_A, FTRL_KAP2_2_8_ANDRE_LEDD)

    val yrkesaktivBestemmelserOgVilkår = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_2, emptySet()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_3_ANDRE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_A, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_B, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_C, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_D, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_E, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_F, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_G, emptySet()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_A, emptySet()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_B, emptySet()),
        Pair(FTRL_KAP2_2_6_FØRSTE_LEDD_C, emptySet()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_7A, emptySet()),
        Pair(
            FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID)
        ),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            LinkedHashSet(listOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
        ),
    )

    val ikkeYrkesaktivBestemmelserOgVilkår = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_C, emptySet()),
        Pair(FTRL_KAP2_2_5_FØRSTE_LEDD_H, emptySet()),
        Pair(FTRL_KAP2_2_5_ANDRE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_7_FJERDE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_B, emptySet()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_C, emptySet()),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            LinkedHashSet(listOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
        ),
        Pair(FTRL_KAP2_2_8_FJERDE_LEDD, emptySet()),
    )

    val pensjonistBestemmelserOgVilkår = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_1_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_1_FJERDE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_3_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_5_ANDRE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_7_FØRSTE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_7_FJERDE_LEDD, emptySet()),
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_D, emptySet()),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            LinkedHashSet(listOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
        ),
        Pair(FTRL_KAP2_2_8_FJERDE_LEDD, emptySet()),
    )

    val defaultBestemmelserOgVilkår = mapOf<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>>(
        Pair(FTRL_KAP2_2_8_FØRSTE_LEDD_A, setOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID)),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            LinkedHashSet(listOf(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE))
        ),
    )

    private fun bestemmelseOgVilkårFraBehandlingstema(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        when (behandlingstema) {
            Behandlingstema.YRKESAKTIV -> yrkesaktivBestemmelserOgVilkår
            Behandlingstema.IKKE_YRKESAKTIV -> ikkeYrkesaktivBestemmelserOgVilkår
            Behandlingstema.PENSJONIST -> pensjonistBestemmelserOgVilkår
            else -> defaultBestemmelserOgVilkår
        }

    fun hentStøttede(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        bestemmelseOgVilkårFraBehandlingstema(behandlingstema).filter { støttetBestemmelser.contains(it.key) }

    fun hentIkkeStøttede(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        bestemmelseOgVilkårFraBehandlingstema(behandlingstema).filter { !støttetBestemmelser.contains(it.key) }
}
