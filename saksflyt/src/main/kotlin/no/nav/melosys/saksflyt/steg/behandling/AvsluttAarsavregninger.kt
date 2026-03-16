package no.nav.melosys.saksflyt.steg.behandling

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class AvsluttAarsavregninger(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val behandlingsresultatService: BehandlingsresultatService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg? {
        return ProsessSteg.AVSLUTT_AARSAVREGNINGER
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val fagsak = behandling.fagsak
        fagsak.hentAktiveÅrsavregninger().forEach {
            log.info("Avslutter årsavregning med id: ${it.id}")
            oppgaveService.ferdigstillOppgaveMedBehandlingID(it.id)
            behandlingsresultatService.oppdaterBehandlingsresultattype(it.id, Behandlingsresultattyper.FERDIGBEHANDLET)
            behandlingService.avsluttBehandling(it.id)
        }

    }
}
