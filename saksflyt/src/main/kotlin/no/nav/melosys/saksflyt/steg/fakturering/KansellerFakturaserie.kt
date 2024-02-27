package no.nav.melosys.saksflyt.steg.fakturering

import mu.KotlinLogging
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class KansellerFakturaserie(
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.KANSELLER_FAKTURASERIE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
        val fagsak = behandling.fagsak
        val sisteBehandlingMedFakturaserieReferanse =
            fagsak.hentBehandlingerSortertSynkendePåRegistrertDato().firstOrNull { hentFakturaserieReferanse(it.id) != null }

        if (sisteBehandlingMedFakturaserieReferanse != null) {
            val behandlingID = sisteBehandlingMedFakturaserieReferanse.id
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
            log.info("Kansellerer fakturaserie for behandling: $behandlingID med fakturaseriereferanse: ${behandlingsresultat.fakturaserieReferanse}")
            val fakturaserieResponse =
                faktureringskomponentenConsumer.kansellerFakturaserie(behandlingsresultat.fakturaserieReferanse, saksbehandlerIdent)
            behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
            behandlingsresultatService.lagre(behandlingsresultat)
        } else {
            log.info { "Finner ingen fakturaseriereferanse for sak: ${fagsak.saksnummer}" }
        }
    }

    private fun hentFakturaserieReferanse(behandlingID: Long): String? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        return behandlingsresultat.fakturaserieReferanse
    }
}
