package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import org.springframework.stereotype.Component;

@Component
public class LagreMedlemsperiodeMedl implements StegBehandler {

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final BehandlingsresultatService behandlingsresultatService;

    public LagreMedlemsperiodeMedl(MedlemskapsperiodeService medlemskapsperiodeService,
                                   BehandlingsresultatService behandlingsresultatService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        var behandling = prosessinstans.getBehandling();
        long behandlingId = prosessinstans.getBehandling().getId();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);

        var innvilgedeMedlemskapsperioder = behandlingsresultat.finnMedlemskapsperioder()
            .stream().filter(Medlemskapsperiode::erInnvilget).toList();
        if (behandling.erNyVurdering() && behandling.getOpprinneligBehandling() != null) {
            medlemskapsperiodeService.erstattMedlemskapsperioder(innvilgedeMedlemskapsperioder, behandling.getOpprinneligBehandling().getId(), behandlingId);
        } else {
            for (Medlemskapsperiode medlemskapsperiode : innvilgedeMedlemskapsperioder) {
                medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(behandlingId, medlemskapsperiode);
            }
        }
    }
}
