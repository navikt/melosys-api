package no.nav.melosys.service.ftrl

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.stereotype.Service

@Service
class GyldigeTrygdedekningerService(private val behandlingService: BehandlingService, private val unleash: Unleash) {

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

    fun hentTrygdedekninger(behandlingID: Long): List<Trygdedekninger> {
        val behandling = behandlingService.hentBehandling(behandlingID)

        valider(behandling)

        return when {
            behandling.tema == Behandlingstema.IKKE_YRKESAKTIV -> GYLDIGE_TRYGDEDEKNINGER_IKKE_YRKESAKTIV
            unleash.isEnabled(ToggleName.MELOSYS_FOLKETRYGDEN_2_7) -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV
            else -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV_GAMMMEL
        }
    }

    private fun valider(behandling: Behandling) {
        if (!behandling.fagsak.erSakstypeFtrl()) {
            throw FunksjonellException("Behandling ${behandling.id} med sakstype ${behandling.fagsak.type} har ikke gyldige trygdedekninger")
        }
        if (behandling.tema !in listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)) {
            throw FunksjonellException("Behandling ${behandling.id} med behandlingstema ${behandling.tema} har ikke gyldige trygdedekninger")
        }
        if (behandling.tema == Behandlingstema.IKKE_YRKESAKTIV && !unleash.isEnabled(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV)) {
            throw FunksjonellException("Behandling ${behandling.id} med behandlingstema Ikke Yrkesaktiv har ikke gyldige trygdedekninger mens toggle er slått av")
        }
    }
}
