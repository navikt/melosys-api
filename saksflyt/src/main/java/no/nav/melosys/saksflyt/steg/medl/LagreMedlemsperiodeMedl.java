package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

@Component
public class LagreMedlemsperiodeMedl implements StegBehandler {

    private final MedlPeriodeService medlPeriodeService;
    private final BehandlingsresultatService behandlingsresultatService;

    public LagreMedlemsperiodeMedl(MedlPeriodeService medlPeriodeService,
                                   BehandlingsresultatService behandlingsresultatService) {
        this.medlPeriodeService = medlPeriodeService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        long behandlingId = prosessinstans.getBehandling().getId();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        var behandling = behandlingsresultat.getBehandling();

        var innvilgedeMedlemskapsperioder = behandlingsresultat.finnMedlemskapsperioder()
            .stream().filter(Medlemskapsperiode::erInnvilget).toList();
        if (behandling.erNyVurdering()) {
            medlPeriodeService.erstattMedlemskapsperioder(innvilgedeMedlemskapsperioder, behandling.getOpprinneligBehandling().getId(), behandlingId);
        } else {
            if (behandlingsresultat.erAvslag()) {
                return;
            }
            for (Medlemskapsperiode medlemskapsperiode : innvilgedeMedlemskapsperioder) {
                opprettEllerOppdaterMedlPeriode(behandlingId, medlemskapsperiode);
            }
        }
    }

    private void opprettEllerOppdaterMedlPeriode(long behandlingId, Medlemskapsperiode medlemskapsperiode) {
        if (medlemskapsperiode.getMedlPeriodeID() == null) {
            medlPeriodeService.opprettPeriodeEndelig(behandlingId, medlemskapsperiode);
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(behandlingId, medlemskapsperiode);
        }
    }
}
