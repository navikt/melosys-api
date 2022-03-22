package no.nav.melosys.saksflyt.steg.medl;

import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class LagreMedlemsperiodeMedl implements StegBehandler {

    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final MedlPeriodeService medlPeriodeService;
    private final BehandlingsresultatService behandlingsresultatService;

    public LagreMedlemsperiodeMedl(MedlemAvFolketrygdenService medlemAvFolketrygdenService,
                                   MedlPeriodeService medlPeriodeService,
                                   BehandlingsresultatService behandlingsresultatService) {
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.medlPeriodeService = medlPeriodeService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();
        long behandlingId = behandling.getId();
        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        if (resultat.erAvslag()) {
            return;
        }

        var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingId);
        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();

        if (medlemskapsperioder.isEmpty()) {
            throw new FunksjonellException("Ingen medlemskapsperioder funnet for behandling " + behandlingId);
        }

        for (Medlemskapsperiode medlemskapsperiode : medlemskapsperioder) {
            opprettMedlPeriode(behandlingId, medlemskapsperiode);
        }
    }

    private void opprettMedlPeriode(long behandlingId, Medlemskapsperiode medlemskapsperiode) {
        if (isEmpty(medlemskapsperiode.getMedlPeriodeID())) {
            medlPeriodeService.opprettPeriodeEndelig(behandlingId, medlemskapsperiode);
        }
    }
}
