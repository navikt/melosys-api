package no.nav.melosys.service.ftrl.bestemmelse

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.springframework.stereotype.Component

@Component
class BestemmelserFraBehandlingstema {
    fun bestemmelserFraBehandlingstema(behandlingstema: Behandlingstema?): List<Folketrygdloven_kap2_bestemmelser> {
        return when (behandlingstema) {
            Behandlingstema.YRKESAKTIV -> YrkesaktivBestemmelser.bestemmelser
            Behandlingstema.IKKE_YRKESAKTIV -> IkkeYrkesaktivBestemmelser.bestemmelser
            Behandlingstema.PENSJONIST -> PensjonistBestemmelser.bestemmelser
            else -> Folketrygdloven_kap2_bestemmelser.values().toList()
        }
    }
}
