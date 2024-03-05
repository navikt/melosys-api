package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelseYrkesaktivNy(val mottatteOpplysningerService: MottatteOpplysningerService) {
    fun hentVilkår(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> {

        return when (bestemmelse) {
            FTRL_KAP2_2_1 -> ftrlKap2_1VilkårForBehandling(behandlingID, avklarteFakta)

            FTRL_KAP2_2_2 -> ftrlKap2_2VilkårForBehandling(avklarteFakta)

            FTRL_KAP2_2_3_ANDRE_LEDD -> listOf(
                Vilkår(FTRL_ARBEIDSTAKER),
                Vilkår(FTRL_2_3_ARBEIDSGIVER_SVALBARD_JAN_MAYEN)
            )

            FTRL_KAP2_2_5_FØRSTE_LEDD_A -> listOf(
                Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
                Vilkår(FTRL_ARBEIDSTAKER),
                Vilkår(FTRL_2_5_NORSKE_STATS_TJENESTE)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_B -> listOf(
                Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
                Vilkår(FTRL_ARBEIDSTAKER),
                Vilkår(FTRL_2_5_ARBEID_FOR_PERSON_I_NORSKE_STATS_TJENESTE)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_C -> listOf(
                Vilkår(FTRL_2_5_I_FORSVARETS_TJENESTE)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_D -> listOf(
                Vilkår(FTRL_2_5_FREDSKORPSDELTAKER_EKSPERT_UTVIKLINGSLAND)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_E -> listOf(
                Vilkår(FTRL_2_5_NATO_SIVILE_KRIGSTIDSORGANGER)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_F -> listOf(
                Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
                Vilkår(FTRL_ARBEIDSTAKER),
                Vilkår(FTRL_2_5_NORSK_SKIP),
                Vilkår(FTRL_2_12_UNNTAK_TURISTSKIP)
            )
            FTRL_KAP2_2_5_FØRSTE_LEDD_G -> listOf(
                Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
                Vilkår(FTRL_ARBEIDSTAKER),
                Vilkår(FTRL_2_5_NORSK_SIVILT_LUFTFARTSSELSKAP),
            )

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
                Vilkår(FTRL_FORUTGÅENDE_TRYGDETID),
                Vilkår(FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE)
            )

            FTRL_KAP2_2_8_ANDRE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_FORUTGÅENDE_TRYGDETID),
                Vilkår(
                    FTRL_2_8_NÆR_TILKNYTNING_NORGE,
                    muligeBegrunnelser = toStringList(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values())
                )
            )

            else -> emptyList()
        }
    }

    private fun ftrlKap2_2VilkårForBehandling(avklarteFakta: Map<Avklartefaktatyper, String>): List<Vilkår> {
        val arbeidssituasjonType = hentArbeidssituasjonFraFakta(avklarteFakta)

        val vilkårForArbeidssituasjon = ftrlKap2_2VilkårForArbeidssituasjon(arbeidssituasjonType)

        return vilkårForArbeidssituasjon + Vilkår(FTRL_ARBEIDSTAKER) + Vilkår(FTRL_2_2_LOVLIG_ADGANG_ARBEID)
    }

    private fun ftrlKap2_1VilkårForBehandling(behandlingID: Long?, avklarteFakta: Map<Avklartefaktatyper, String>): List<Vilkår> {
        if (behandlingID == null) {
            throw FunksjonellException("BehandlingID trengs for å avgjøre land for ftrlKap2_1")
        }

        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)
        val vilkårForLand = ftrlKap2_1VilkårForLand(mottatteOpplysninger.mottatteOpplysningerData?.soeknadsland)

        if(vilkårForLand.isNotEmpty()){
            return vilkårForLand + Vilkår(FTRL_2_1_LOVLIG_OPPHOLD)
        }

        val arbeidssituasjonType = hentArbeidssituasjonFraFakta(avklarteFakta)
        val vilkårForArbeidssituasjon = ftrlKap2_1VilkårForArbeidssituasjon(arbeidssituasjonType)

        return vilkårForArbeidssituasjon + Vilkår(FTRL_2_14_ARBEIDSGIVERAVGIFT)
    }


    private fun ftrlKap2_2VilkårForArbeidssituasjon(arbeidssituasjontype: Arbeidssituasjontype?) : List<Vilkår> {
        if (arbeidssituasjontype == null) {
            return emptyList()
        }
        return when(arbeidssituasjontype) {
            Arbeidssituasjontype.ARBIED_I_NORGE_2_2 -> listOf(
                Vilkår(FTRL_2_11_UNNTAK_AMBASSADEPERSONELL_MELLOMFOLKELIG_ORG)
            )
            Arbeidssituasjontype.ARBEID_PÅ_NORSK_SOKKEL_2_2 -> listOf(
                Vilkår(FTRL_2_2_INNRETNING_NATURRESSURSER)
            )
            else -> emptyList()
        }
    }

    private fun ftrlKap2_1VilkårForArbeidssituasjon(arbeidssituasjontype: Arbeidssituasjontype?) : List<Vilkår> {
        if (arbeidssituasjontype == null) {
            return emptyList()
        }
        return when(arbeidssituasjontype) {
            Arbeidssituasjontype.MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD -> listOf(
                Vilkår(FTRL_2_1_BOSATT_NORGE_FORUT),
                Vilkår(FTRL_2_1_ARBEID_OPPHOLD_UNDER_12MND)
            )
            Arbeidssituasjontype.VEKSELVIS_ARBEID_2_1_FJERDE_LEDD -> listOf(
                Vilkår(FTRL_2_1_VEKSELSVIS_ARBEIDSPERIODE_UNDER_12MND),
                Vilkår(FTRL_2_14_FRITID_I_NORGE)
            )
            else -> emptyList()
        }
    }


    private fun ftrlKap2_1VilkårForLand(søknadsland: Soeknadsland?): List<Vilkår> {
        if (søknadsland == null) {
            return emptyList()
        }

        val kunNorge = søknadsland.landkoder.first() == Land_iso2.NO.toString() && søknadsland.landkoder.size == 1

        return if(kunNorge) {
            listOf(Vilkår(FTRL_2_1_BOSATT_NORGE), Vilkår(FTRL_2_11_UNNTAK_AMBASSADEPERSONELL_MELLOMFOLKELIG_ORG))
        } else {
            listOf()
        }
    }

    private fun hentArbeidssituasjonFraFakta(avklarteFakta: Map<Avklartefaktatyper, String>): Arbeidssituasjontype {
        val avklarteArbeidssituasjon = avklarteFakta[Avklartefaktatyper.ARBEIDSSITUASJON]
        if (avklarteArbeidssituasjon == null || !Arbeidssituasjontype.values().any { it.name == avklarteArbeidssituasjon }) {
            throw FunksjonellException("Arbeidssituasjon $avklarteArbeidssituasjon er ugyldig")
        }
        return Arbeidssituasjontype.valueOf(avklarteArbeidssituasjon)
    }

    companion object {
        internal fun toStringList(vararg kodeverkVerdier: Kodeverk): Collection<String> = kodeverkVerdier.map { it.kode }.toList()
    }
}
