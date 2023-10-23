package no.nav.melosys.saksflyt.faktureringskomponenten.behandlingsresultat

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class LagreFakturaserieReferanse(private val behandlingsresultatService: BehandlingsresultatService) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.LAGRE_FAKTURASERIE_REFERANSE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE) ?: return
        val behandlingsId = prosessinstans.behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)

        behandlingsresultat.fakturaserieReferanse = fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)

        log.info("Lagret ny fakturaserie referanse {} på behandlingsresultat {}", fakturaserieReferanse, behandlingsId)
    }
}
