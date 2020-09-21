package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPDATER_MEDL_ANMODNING_UNNTAK;

@Component
public class OppdaterMedlAnmodningUnntak implements StegBehandler {

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OppdaterMedlAnmodningUnntak(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPDATER_MEDL_ANMODNING_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Anmodningsperiode anmodningsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertAnmodningsperiode();
        medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiode, behandlingID, false);
    }
}
