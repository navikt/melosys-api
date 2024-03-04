package no.nav.melosys.service.ftrl.bestemmelse

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Component

@Component
class FtrlBestemmelser(private val unleash: Unleash) {

    fun hentBestemmelser(behandlingstema: Behandlingstema?, trygdedekning: Trygdedekninger?): List<Folketrygdloven_kap2_bestemmelser> {
        val bestemmelserFraBehandlingstema = bestemmelserFraBehandlingstema(behandlingstema)
        val pliktigeBestemmelserToggleAktiv = unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER)

        if (trygdedekning != null) {
            val bestemmelserFraTrygdedekning = if(pliktigeBestemmelserToggleAktiv)
                LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelserToggle(trygdedekning).toSet()
            else
                LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelser(trygdedekning).toSet()

            return bestemmelserFraBehandlingstema.intersect(bestemmelserFraTrygdedekning).toList()
        }

        return bestemmelserFraBehandlingstema
    }

    private fun bestemmelserFraBehandlingstema(behandlingstema: Behandlingstema?): List<Folketrygdloven_kap2_bestemmelser> {
        val pliktigeBestemmelserToggleAktiv = unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER)

        return when (behandlingstema) {
            Behandlingstema.YRKESAKTIV ->  if(pliktigeBestemmelserToggleAktiv) YrkesaktivBestemmelser.bestemmelserNy else YrkesaktivBestemmelser.bestemmelser
            Behandlingstema.IKKE_YRKESAKTIV -> if(pliktigeBestemmelserToggleAktiv) IkkeYrkesaktivBestemmelser.bestemmelserNy else IkkeYrkesaktivBestemmelser.bestemmelser
            Behandlingstema.PENSJONIST -> if(pliktigeBestemmelserToggleAktiv) PensjonistBestemmelser.bestemmelserNy else PensjonistBestemmelser.bestemmelser
            else -> Folketrygdloven_kap2_bestemmelser.values().toList()
        }
    }
}
