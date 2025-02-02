package no.nav.melosys.saksflyt.steg.satsendring

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import org.springframework.stereotype.Component
import java.time.LocalDate

val log = KotlinLogging.logger { }

@Component
class OpprettSatsbehandling(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_SATSBEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandlingSomSkalReplikeres =
            requireNotNull(prosessinstans.behandling) { "Behandling mangler i prosessinstans: ${prosessinstans.id}" }
        val nyBehandling: Behandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            behandlingSomSkalReplikeres,
            Behandlingstyper.SATSENDRING
        )

        val behandlingsårsak = Behandlingsaarsak(Behandlingsaarsaktyper.ÅRLIG_SATSOPPDATERING, null, LocalDate.now())
        nyBehandling.settBehandlingsårsak(behandlingsårsak)
        nyBehandling.behandlingsfrist = LocalDate.now()

        oppdaterBehandlingsresultat(nyBehandling)

        prosessinstans.behandling = nyBehandling

        behandlingService.lagre(nyBehandling)

        log.info(
            "Behandling {} replikert og satsendringsbehandling {} har blitt opprettet for {}",
            behandlingSomSkalReplikeres.id, nyBehandling.id, nyBehandling.fagsak.saksnummer
        )
    }

    private fun oppdaterBehandlingsresultat(nyBehandling: Behandling) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(nyBehandling.id)
        behandlingsresultat.behandlingsmåte = Behandlingsmaate.AUTOMATISERT
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
        behandlingsresultat.settVedtakMetadata(Vedtakstyper.ENDRINGSVEDTAK, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
    }

}
