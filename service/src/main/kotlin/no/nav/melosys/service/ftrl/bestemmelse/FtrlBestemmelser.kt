package no.nav.melosys.service.ftrl.bestemmelse

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Component

@Component
class FtrlBestemmelser() {

    fun hentBestemmelser(behandlingstema: Behandlingstema?, trygdedekning: Trygdedekninger?): List<Folketrygdloven_kap2_bestemmelser> {
        val bestemmelserFraBehandlingstema = bestemmelserFraBehandlingstema(behandlingstema)

        if (trygdedekning != null) {
            val bestemmelserFraTrygdedekning = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelser(trygdedekning).toSet()

            return bestemmelserFraBehandlingstema.intersect(bestemmelserFraTrygdedekning).toList()
        }

        return bestemmelserFraBehandlingstema
    }

    private fun bestemmelserFraBehandlingstema(behandlingstema: Behandlingstema?): List<Folketrygdloven_kap2_bestemmelser> {
        return when (behandlingstema) {
            Behandlingstema.YRKESAKTIV ->  YrkesaktivBestemmelser.bestemmelser
            Behandlingstema.IKKE_YRKESAKTIV -> IkkeYrkesaktivBestemmelser.bestemmelser
            Behandlingstema.PENSJONIST -> PensjonistBestemmelser.bestemmelser
            else -> Folketrygdloven_kap2_bestemmelser.values().toList()
        }
    }
}
