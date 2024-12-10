package no.nav.melosys.service.ftrl

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Bestemmelse
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

    private val GYLDIGE_TRYGDEDEKNINGER_SPESIELLE_GRUPPER = listOf(
        Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL,
    )

    fun hentTrygdedekninger(behandlingstema: Behandlingstema, bestemmelse: Bestemmelse?): List<Trygdedekninger> {
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
        var trygdedekninger = when (behandlingstema) {
            Behandlingstema.IKKE_YRKESAKTIV -> GYLDIGE_TRYGDEDEKNINGER_IKKE_YRKESAKTIV
            Behandlingstema.YRKESAKTIV -> GYLDIGE_TRYGDEDEKNINGER_YRKESAKTIV
            else -> throw FunksjonellException("Finnes ikke gyldige trygdedekninger for behandlingstema $behandlingstema")
        }
        if (unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESSKADEFORDEL)) {
            trygdedekninger = trygdedekninger + TILLEGG_GYLDIGE_TRYGDEDEKNINGER_YRESSKADEFORDEL
        }
        if (unleash.isEnabled(ToggleName.MELOSYS_SPESIELLE_GRUPPER)) {
            trygdedekninger = trygdedekninger + GYLDIGE_TRYGDEDEKNINGER_SPESIELLE_GRUPPER
        }
        return trygdedekninger
    }

    private fun valider(behandlingstema: Behandlingstema) {
        if (behandlingstema !in listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)) {
            throw FunksjonellException("Behandling med behandlingstema $behandlingstema har ikke gyldige trygdedekninger")
        }
    }
}
