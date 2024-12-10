package no.nav.melosys.service.ftrl.bestemmelse.avklartefakta

import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.springframework.stereotype.Component

@Component
class AvklarteFaktaForBestemmelse(
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val behandlingService: BehandlingService
) {

    fun hentAvklarteFakta(bestemmelse: Bestemmelse, behandlingID: Long): List<AvklarteFaktaType> {
        val erIkkeYrkesaktiv = behandlingService.hentBehandling(behandlingID).tema == Behandlingstema.IKKE_YRKESAKTIV

        return if (erIkkeYrkesaktiv) hentAvklarteFaktaIkkeYrkesaktiv(bestemmelse, behandlingID) else hentAvklarteFaktaYrkesaktiv(
            bestemmelse,
            behandlingID
        )
    }

    fun hentAvklarteFaktaIkkeYrkesaktiv(bestemmelse: Bestemmelse, behandlingID: Long): List<AvklarteFaktaType> {
        return when (bestemmelse) {
            FTRL_KAP2_2_1 -> ftrlKap2_1AvklarteFaktaForBehandling(behandlingID, Behandlingstema.IKKE_YRKESAKTIV)

            FTRL_KAP2_2_5_ANDRE_LEDD -> listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                        Ikkeyrkesaktivrelasjontype.BARN_2_5_ANDRE_LEDD,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_C_TIL_E,
                    ).map(Ikkeyrkesaktivrelasjontype::name)
                )
            )

            FTRL_KAP2_2_8_FJERDE_LEDD -> listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                        Ikkeyrkesaktivrelasjontype.BARN_2_8_FJERDE_LEDD,
                        Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD,
                    ).map(Ikkeyrkesaktivrelasjontype::name)
                )
            )

            else -> emptyList()
        }
    }

    fun hentAvklarteFaktaYrkesaktiv(bestemmelse: Bestemmelse, behandlingID: Long): List<AvklarteFaktaType> {
        return when (bestemmelse) {
            FTRL_KAP2_2_1 -> ftrlKap2_1AvklarteFaktaForBehandling(behandlingID, Behandlingstema.YRKESAKTIV)
            FTRL_KAP2_2_2 -> listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.ARBEIDSSITUASJON, listOf(
                        Arbeidssituasjontype.ARBIED_I_NORGE_2_2,
                        Arbeidssituasjontype.ARBEID_PÅ_NORSK_SOKKEL_2_2
                    ).map(Arbeidssituasjontype::name)
                )
            )

            else -> emptyList()
        }
    }

    private fun ftrlKap2_1AvklarteFaktaForBehandling(behandlingID: Long, behandlingstema: Behandlingstema): List<AvklarteFaktaType> {
        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)
        return ftrlKap2_1AvklarteFaktaForLand(mottatteOpplysninger.mottatteOpplysningerData?.soeknadsland, behandlingstema)
    }

    private fun ftrlKap2_1AvklarteFaktaForLand(søknadsland: Soeknadsland?, behandlingstema: Behandlingstema): List<AvklarteFaktaType> {
        if (søknadsland == null) {
            return emptyList()
        }
        val ettEllerFlereLandUtenforNorge = søknadsland.landkoder.any { it != Land_iso2.NO.toString() } || søknadsland.isFlereLandUkjentHvilke

        if (!ettEllerFlereLandUtenforNorge) {
            return emptyList()
        }
        // Ett eller flere land utenfor Norge. Vi legger alltid til grunn at det gjelder opphold i utlandet ved flere land.
        if (ettEllerFlereLandUtenforNorge) {
            return if (behandlingstema == Behandlingstema.YRKESAKTIV) listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.ARBEIDSSITUASJON,
                    listOf(
                        Arbeidssituasjontype.MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD,
                        Arbeidssituasjontype.VEKSELVIS_ARBEID_2_1_FJERDE_LEDD
                    ).map(Arbeidssituasjontype::name)
                )
            ) else listOf(
                AvklarteFaktaType(
                    Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD,
                    listOf(
                        Ikkeyrkesaktivoppholdtype.MIDLERTIDIG_2_1_FJERDE_LEDD,
                        Ikkeyrkesaktivoppholdtype.VEKSELVIS_2_1_FJERDE_LEDD
                    ).map(Ikkeyrkesaktivoppholdtype::name)
                )
            )
        }
        return emptyList()
    }

    data class AvklarteFaktaType(val type: Avklartefaktatyper, val muligeFakta: List<String>)
}
