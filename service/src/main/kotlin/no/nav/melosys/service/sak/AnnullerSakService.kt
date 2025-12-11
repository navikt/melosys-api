package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service

@Service
class AnnullerSakService(
    private val prosessinstansService: ProsessinstansService,
    private val fagsakService: FagsakService,
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService,
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val lovvalgsperiodeService: LovvalgsperiodeService
) {
    fun annullerSak(saksnummer: String) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val behandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
        if (behandling.erEøsPensjonist()) {
            helseutgiftDekkesPeriodeService.slettHelseutgiftDekkesPeriode(behandlingsresultat.hentId())
        } else if (behandling.fagsak.erLovvalg()) {
            lovvalgsperiodeService.slettLovvalgsperioder(behandlingsresultat.hentId())
        } else {
            medlemskapsperiodeService.slettMedlemskapsperioder(behandlingsresultat.hentId())
        }

        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingsresultat.hentId(), Behandlingsresultattyper.ANNULLERT)
        prosessinstansService.opprettAnnullerFagsakProsessflyt(behandling)
    }

}
