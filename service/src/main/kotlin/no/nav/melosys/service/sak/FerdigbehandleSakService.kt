package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FerdigbehandleSakService(
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService,
) {
    @Transactional
    fun ferdigbehandleSak(saksnummer: String) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
        val nyStatus = if (fagsak.status == Saksstatuser.OPPRETTET) Saksstatuser.AVSLUTTET else fagsak.status

        fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, nyStatus)
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET)
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }
}
