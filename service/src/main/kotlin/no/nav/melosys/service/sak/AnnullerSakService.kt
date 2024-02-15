package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service

@Service
class AnnullerSakService(
    private val prosessinstansService: ProsessinstansService,
    private val fagsakService: FagsakService,
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService,
) {
    fun annullerSak(saksnummer: String) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentAktivBehandling()
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer)
        if (behandlingsresultat.medlemAvFolketrygden != null) {
            medlemskapsperiodeService.slettMedlemskapsperioder(behandlingsresultat.id)
        }
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingsresultat.id, Behandlingsresultattyper.ANNULLERT)
        prosessinstansService.opprettAnnullerFagsakProsessflyt(behandling)
    }

}
