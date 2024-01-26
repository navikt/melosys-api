package no.nav.melosys.service.medlemskapsperiode

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.util.KodeverkUtils
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Service

@Service
class UtledBestemmelserOgVilkår (val unleash: Unleash) {
    val støttetBestemmelser2_8 = listOf(
        FTRL_KAP2_2_8_FØRSTE_LEDD_A,
        FTRL_KAP2_2_8_ANDRE_LEDD
    )
    val støttetBestemmelser2_7 = listOf(
        FTRL_KAP2_2_7_FØRSTE_LEDD,
        FTRL_KAP2_2_7A
    )

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
        Pair(
            FTRL_KAP2_2_7_FØRSTE_LEDD,
            LinkedHashSet(
                listOf(
                    Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING,
                    Vilkaar.FTRL_2_7_IKKE_PLIKTIG_MEDLEM,
                    Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING
                )
            )
        ),
        Pair(
            FTRL_KAP2_2_7A,
            LinkedHashSet(
                listOf(
                    Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING,
                    Vilkaar.FTRL_2_7A_BOSATT_I_NORGE,
                    Vilkaar.FTRL_2_7A_SKIP_UTENFOR_EØS
                )
            )
        ),
        Pair(
            FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            LinkedHashSet(
                listOf(
                    Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING,
                    Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID,
                    Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
                )
            )
        ),
        Pair(
            FTRL_KAP2_2_8_ANDRE_LEDD,
            LinkedHashSet(
                listOf(
                    Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING,
                    Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID,
                    Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
                )
            )
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

    fun hentStøttedeBestemmelserOgVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        bestemmelseOgVilkårFraBehandlingstema(behandlingstema).filter {
            (unleash.isEnabled(ToggleName.MELOSYS_FOLKETRYGDEN_2_7) && støttetBestemmelser2_7.contains(it.key))
                || støttetBestemmelser2_8.contains(it.key)
        }

    fun hentIkkeStøttedeBestemmelserOgVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        //if toggle is on, do the same for støttetBestemmelser2_7 as well, else only støttetBestemmelser2_8 (filter)
        bestemmelseOgVilkårFraBehandlingstema(behandlingstema).filter {
            !støttetBestemmelser2_8.contains(it.key) && (!unleash.isEnabled(ToggleName.MELOSYS_FOLKETRYGDEN_2_7) || !støttetBestemmelser2_7.contains(it.key))
        }

    fun hentBegrunnelserForVilkår(vilkår: Vilkaar): Collection<String> =
        if (vilkår == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE) {
            KodeverkUtils.tilStringCollection(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values())
        } else emptyList()

}
