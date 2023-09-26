package behandlingsresultat

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.ProsessSteg
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class LagreFakturaserieReferanse(private val behandlingsresultatService: BehandlingsresultatService) : StegBehandler {

    companion object {
        private val log = LoggerFactory.getLogger(LagreFakturaserieReferanse::class.java)
    }

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.LAGRE_FAKTURASERIE_REFERANSE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE) == null) return

        val behandlingsId = prosessinstans.behandling.id
        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)
        val behandlingsresultat: Behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)

        behandlingsresultat.fakturaserieReferanse = fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
        
        log.info("Lagret ny fakturaserie referanse {} på behandlingsresultat {}", fakturaserieReferanse, behandlingsId)
    }
}
