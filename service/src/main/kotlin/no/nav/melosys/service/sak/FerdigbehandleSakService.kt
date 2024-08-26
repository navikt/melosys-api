package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FerdigbehandleSakService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService
) {
    @Deprecated("erstattet av med metode med behandlingId")
    @Transactional
    fun ferdigbehandleSak(saksnummer: String) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
        val nyStatus = if (fagsak.status == Saksstatuser.OPPRETTET) Saksstatuser.AVSLUTTET else fagsak.status

        fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, nyStatus)
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET)
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }

    @Transactional
    fun ferdigbehandle(behandlingId: Long) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsak.saksnummer)

        if (behandling.type == Behandlingstyper.ÅRSAVREGNING) {
            behandlingsresultatService.tømBehandlingsresultat(behandlingId)
        }

        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingId, Behandlingsresultattyper.FERDIGBEHANDLET)

        if (fagsak.erEnesteBehandling(behandlingId)) {
            fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET)
        } else {
            behandlingService.avsluttBehandling(behandlingId)
        }

        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingId)
    }
}
