package no.nav.melosys.service.ftrl.bestemmelse

import io.getunleash.Unleash
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Component

@Component
class FtrlBestemmelser(
    private val unleash: Unleash
) {
    fun hentBestemmelser(behandlingstema: Behandlingstema?, trygdedekning: Trygdedekninger?): List<Bestemmelse> {
        val bestemmelserFraBehandlingstema = bestemmelserFraBehandlingstema(behandlingstema)

        if (trygdedekning != null) {
            val bestemmelserFraTrygdedekning = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelser(trygdedekning).toSet()
            return bestemmelserFraBehandlingstema.intersect(bestemmelserFraTrygdedekning).toList()
        }

        return bestemmelserFraBehandlingstema
    }

    private fun bestemmelserFraBehandlingstema(behandlingstema: Behandlingstema?): List<Bestemmelse> {
        return when (behandlingstema) {
            Behandlingstema.YRKESAKTIV -> if (unleash.isEnabled(ToggleName.MELOSYS_SPESIELLE_GRUPPER)) {
                YrkesaktivBestemmelser.bestemmelserMedSpesielleGrupper
            } else {
                YrkesaktivBestemmelser.bestemmelser
            }

            Behandlingstema.IKKE_YRKESAKTIV -> IkkeYrkesaktivBestemmelser.bestemmelser
            Behandlingstema.PENSJONIST -> PensjonistBestemmelser.bestemmelser
            else -> Folketrygdloven_kap2_bestemmelser.values().toList()
        }
    }
}
