package no.nav.melosys.service.eessi.ruting;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AdminSedRuter {
    private static final Logger log = LoggerFactory.getLogger(AdminSedRuter.class);

    private final FagsakService fagsakService;
    protected final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final ProsessinstansService prosessinstansService;
    protected final OppgaveService oppgaveService;

    public AdminSedRuter(FagsakService fagsakService,
                         BehandlingsresultatService behandlingsresultatService,
                         MedlPeriodeService medlPeriodeService,
                         ProsessinstansService prosessinstansService, OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
    }

    protected void avvisMedPeriodeOpphørt(Behandling behandling) {
        behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .finnValidertPeriodeOmLovvalg()
            .filter(periodeOmLovvalg -> periodeOmLovvalg.getMedlPeriodeID() != null)
            .ifPresent(periodeOmLovvalg -> medlPeriodeService.avvisPeriodeOpphørt(periodeOmLovvalg.getMedlPeriodeID()));
    }

    public void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling behandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            behandling,
            melosysEessiMelding
        );
    }

    protected Optional<Fagsak> hentFagsakDersomArkivsakIDEksisterer(Long arkivsakID) {
        return arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : (Optional.empty());
    }

    protected void annullerSakOgBehandling(Behandling behandling) {
        if (behandling.erAktiv()) {
            log.info("Behandling {} vil bli avsluttet og status settes til annullert", behandling.getId());
            fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.ANNULLERT);
        } else {
            log.info("Saksstatus settes til annullert for behandling {}", behandling.getId());
            fagsakService.oppdaterStatus(behandling.getFagsak(), Saksstatuser.ANNULLERT);
        }
        avvisMedPeriodeOpphørt(behandling);
    }
}
