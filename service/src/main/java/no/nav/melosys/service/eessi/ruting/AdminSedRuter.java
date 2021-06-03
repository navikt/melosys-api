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

    String annullerSakOgBehandling(Behandling behandling) {
        Optional<? extends PeriodeOmLovvalg> periodeOmLovvalgMedMedlPeriode;
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        String info;

        if (behandling.erAktiv()) {
            info = String.format("Behandling %2d vil bli avsluttet og status settes til annullert", behandling.getId());
            fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.ANNULLERT);
            periodeOmLovvalgMedMedlPeriode = behandlingsresultat
                .finnValidertAnmodningsperiode()
                .filter(a -> a.getMedlPeriodeID() != null);
        } else {
            info = String.format("Saksstatus settes til annullert for behandling %2d", behandling.getId());
            fagsakService.oppdaterStatus(behandling.getFagsak(), Saksstatuser.ANNULLERT);
            periodeOmLovvalgMedMedlPeriode = behandlingsresultat
                .finnValidertLovvalgsperiode()
                .filter(l -> l.getMedlPeriodeID() != null);
        }

        periodeOmLovvalgMedMedlPeriode.ifPresent(periode -> medlPeriodeService.avvisPeriodeOpphørt(periode.getMedlPeriodeID()));
        return info;
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
