package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class VilkårForBestemmelsePensjonist(val mottatteOpplysningerService: MottatteOpplysningerService) {
    fun hentVilkår(
        bestemmelse: Bestemmelse,
        avklarteFakta: Map<Avklartefaktatyper, String>,
        behandlingID: Long?
    ): List<Vilkår> {
        return when (bestemmelse) {
            FTRL_KAP2_2_1 -> ftrlKap2_1VilkårForBehandling(behandlingID)

            FTRL_KAP2_2_5_ANDRE_LEDD -> ftrlKap2_5VilkårForAvklarteFakta(avklarteFakta)

            FTRL_KAP2_2_7_FØRSTE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_7_IKKE_PLIKTIG_MEDLEM),
                Vilkår(FTRL_2_7_RIMELIGHETSVURDERING)
            )

            FTRL_KAP2_2_7_FJERDE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_7_FORSØRGET_FAMILIEMEDLEM),
                Vilkår(FTRL_2_7_INGEN_SÆRLIGE_GRUNNER_TALER_IMOT)
            )

            FTRL_KAP2_2_8_FØRSTE_LEDD_D -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_8_PENSJON_UFØRETRYGD_FOLKETRYGDEN),
                Vilkår(FTRL_2_8_PENSJONIST_TRETTI_ÅR_TRYGDETID),
                Vilkår(FTRL_2_8_PENSJONIST_TI_ÅR_TRYGDETID_FØR_SØKNADSTIDSPUNKT),
                Vilkår(FTRL_2_8_NÆR_TILKNYTNING_NORGE)
            )

            FTRL_KAP2_2_8_ANDRE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_FORUTGÅENDE_TRYGDETID),
                Vilkår(FTRL_2_8_NÆR_TILKNYTNING_NORGE)
            )

            FTRL_KAP2_2_8_FJERDE_LEDD -> listOf(
                Vilkår(FTRL_2_1A_TRYGDEKOORDINGERING),
                Vilkår(FTRL_2_8_FORSØRGET_FAMILIEMEDLEM),
                Vilkår(FTRL_FORUTGÅENDE_TRYGDETID),
                Vilkår(FTRL_2_8_NÆR_TILKNYTNING_NORGE),
            )

            else -> emptyList()
        }
    }

    private fun ftrlKap2_1VilkårForBehandling(behandlingID: Long?): List<Vilkår> {
        if (behandlingID == null) {
            throw FunksjonellException("BehandlingID trengs for å avgjøre land for ftrlKap2_1")
        }
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

    private fun ftrlKap2_5VilkårForAvklarteFakta(avklarteFakta: Map<Avklartefaktatyper, String>): List<Vilkår> {
        val familieRelasjon = hentFamilieRelasjonFraFakta(avklarteFakta)
        return when (familieRelasjon) {
            Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B -> listOf(
                Vilkår(FTRL_2_5_MEDFØLGENDE_A_E, defaultOppfylt = true),
                Vilkår(FTRL_2_5_FORSØRGET_FAMILIEMEDLEM),
                Vilkår(FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER)
            )

            Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_C_TIL_E -> listOf(
                Vilkår(FTRL_2_5_MEDFØLGENDE_A_E, defaultOppfylt = true),
                Vilkår(FTRL_2_5_FORSØRGET_FAMILIEMEDLEM),
                Vilkår(FTRL_FORUTGÅENDE_TRYGDETID)
            )

            else -> emptyList()
        }
    }

    private fun hentFamilieRelasjonFraFakta(avklarteFakta: Map<Avklartefaktatyper, String>): Ikkeyrkesaktivrelasjontype {
        val avklarteFamilieRelasjon = avklarteFakta[Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON]
        if (avklarteFamilieRelasjon == null || !Ikkeyrkesaktivrelasjontype.values().any { it.name == avklarteFamilieRelasjon }) {
            throw FunksjonellException("FamilieRelasjon $avklarteFamilieRelasjon er ugyldig")
        }
        return Ikkeyrkesaktivrelasjontype.valueOf(avklarteFamilieRelasjon)
    }
}
