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

    public AdminSedRuter(FagsakService fagsakService,
                         BehandlingsresultatService behandlingsresultatService,
                         MedlPeriodeService medlPeriodeService,
                         ProsessinstansService prosessinstansService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.prosessinstansService = prosessinstansService;
    }

    protected void avsluttBehandlingOgAvvisMedlPeriodeOpphørtFraAnmodningsperiode(Behandling behandling){
        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.ANNULLERT);
        behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .finnValidertAnmodningsperiode()
            .filter(a -> a.getMedlPeriodeID() != null)
            .ifPresent(anmodningsperiode -> medlPeriodeService.avvisPeriodeOpphørt(anmodningsperiode.getMedlPeriodeID()));
    }

    protected void oppdaterStatusOgAvvisPeriodeMedlPeriodeOpphørtFraLovvalgsperiode(Behandling behandling){
        fagsakService.oppdaterStatus(behandling.getFagsak(),Saksstatuser.ANNULLERT);
        behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .finnValidertLovvalgsperiode()
            .filter(l -> l.getMedlPeriodeID() != null)
            .ifPresent(lovvalgsperiode -> medlPeriodeService.avvisPeriodeOpphørt(lovvalgsperiode.getMedlPeriodeID()));
    }

    public void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    protected MelosysEessiMelding hentMelosysEessiMelding(Prosessinstans prosessinstans) {
        return prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
    }

    protected Optional<Fagsak> hentFagsakDersomArkivsakIDEksisterer(Long arkivsakID) {
        return arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : Optional.empty();
    }
}
