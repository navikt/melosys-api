package no.nav.melosys.saksflyt.steg.fakturering

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class KansellerFakturaserie(
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.KANSELLER_FAKTURASERIE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val behandlingsId = behandling.id
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)

        if (harOpprinneligBehandlingMedTrygdeavgift(behandling)) {
            log.info("Kansellerer fakturaserie for behandling: $behandlingsId med fakturaseriereferanse: ${behandlingsresultat.fakturaserieReferanse}")
            val fakturaserieResponse =
                faktureringskomponentenConsumer.kansellerFakturaserie(behandlingsresultat.fakturaserieReferanse, saksbehandlerIdent)
            behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
            behandlingsresultatService.lagre(behandlingsresultat)
        } else {
            log.info { "Finner ingen fakturaseriereferanse for behandling: $behandlingsId" }
        }
    }

    private fun harOpprinneligBehandlingMedTrygdeavgift(behandling: Behandling): Boolean =
        behandling.opprinneligBehandling?.let {
            trygdeavgiftOppsummeringService.harTrygdeavgiftOgBestiltFaktura(behandlingsresultatService.hentBehandlingsresultat(it.id))
        } ?: false
}
