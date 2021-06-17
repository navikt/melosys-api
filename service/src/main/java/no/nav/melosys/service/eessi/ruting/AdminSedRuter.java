package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;

import java.util.Optional;

public abstract class AdminSedRuter {

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final ProsessinstansService prosessinstansService;
    private final Logger log;

    public AdminSedRuter(FagsakService fagsakService,
                         BehandlingsresultatService behandlingsresultatService,
                         MedlPeriodeService medlPeriodeService,
                         ProsessinstansService prosessinstansService,
                         Logger log) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.prosessinstansService = prosessinstansService;
        this.log = log;
    }

    protected void avvisMedPeriodeOpphørt(Behandling behandling) {
        behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .finnValidertPeriodeOmLOvvalg()
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
        return Optional.ofNullable(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).orElse(Optional.empty());
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
