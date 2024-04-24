package no.nav.melosys.service.ftrl

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import org.springframework.stereotype.Service

@Service
class GyldigeTrygdedekningerService(private val unleash: Unleash) {

    private val GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV = listOf(
        Trygdedekninger.FULL_DEKNING_FTRL,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
    )

    private val GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV_GAMMMEL = listOf(
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
    )

    private val GYLDIGE_TRYGDEDEKNINGER_IKKE_YRKESAKTIV = listOf(
        Trygdedekninger.FULL_DEKNING_FTRL,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
    )

    private val TILLEGG_GYLDIGE_TRYGDEDEKNINGER_YRESSKADEFORDEL = listOf(
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE,
    )

    fun hentTrygdedekninger(behandlingstema: Behandlingstema, bestemmelse: Folketrygdloven_kap2_bestemmelser?): List<Trygdedekninger> {
        valider(behandlingstema)

        val trygdedekningerFraBehandlingstema = trygdedekningerFraBehandlingstema(behandlingstema)

        if (bestemmelse != null) {
            val trygdedekningerFraBestemmelse = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeTrygdedekninger(bestemmelse).toSet()
            return trygdedekningerFraBehandlingstema
                .toMutableList()
                .apply {
                    if (unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESSKADEFORDEL))
                        add(Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE)
                }.intersect(trygdedekningerFraBestemmelse).toList()
        }

        return trygdedekningerFraBehandlingstema
    }

    private fun trygdedekningerFraBehandlingstema(behandlingstema: Behandlingstema): List<Trygdedekninger> {
        val trygdedekninger = when {
            behandlingstema == Behandlingstema.IKKE_YRKESAKTIV -> GYLDIGE_TRYGDEDEKNINGER_IKKE_YRKESAKTIV
            unleash.isEnabled(ToggleName.MELOSYS_FOLKETRYGDEN_2_7) -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV
            unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER) -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV
            else -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV_GAMMMEL
        }
        if (unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESSKADEFORDEL)) {
            return trygdedekninger + TILLEGG_GYLDIGE_TRYGDEDEKNINGER_YRESSKADEFORDEL
        }
        return trygdedekninger
    }

    private fun valider(behandlingstema: Behandlingstema) {
        if (behandlingstema !in listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)) {
            throw FunksjonellException("Behandling med behandlingstema $behandlingstema har ikke gyldige trygdedekninger")
        }
        if (behandlingstema == Behandlingstema.IKKE_YRKESAKTIV && !unleash.isEnabled(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV)) {
            throw FunksjonellException("Behandling med behandlingstema Ikke Yrkesaktiv har ikke gyldige trygdedekninger mens toggle er slått av")
        }
    }
}
