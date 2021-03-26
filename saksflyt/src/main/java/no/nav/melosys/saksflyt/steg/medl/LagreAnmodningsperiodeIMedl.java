package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.LAGRE_ANMODNINGSPERIODE_MEDL;

@Component
public class LagreAnmodningsperiodeIMedl implements StegBehandler {

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public LagreAnmodningsperiodeIMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return LAGRE_ANMODNINGSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        Anmodningsperiode anmodningsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertAnmodningsperiode();
        medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiode, behandlingID, behandling.erBehandlingAvSed());
    }
}
