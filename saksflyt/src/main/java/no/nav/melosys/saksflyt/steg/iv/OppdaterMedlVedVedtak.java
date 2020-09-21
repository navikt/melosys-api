package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPDATER_MEDL_VED_VEDTAK;

@Component
public class OppdaterMedlVedVedtak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedlVedVedtak.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OppdaterMedlVedVedtak(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPDATER_MEDL_VED_VEDTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = prosessinstans.getBehandling();

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (behandlingsresultat.medlOppdateres()) {
            oppdaterMedl(behandlingsresultat);
        }
    }

    private void oppdaterMedl(Behandlingsresultat behandlingsresultat) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingsresultat.getBehandling();
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        if (lovvalgsperiode.getMedlPeriodeID() != null) {
            oppdaterEksisterendeMedlPeriode(lovvalgsperiode);
        } else if (behandlingsresultat.erInnvilgelseFlereLand() || behandlingsresultat.erUtpeking()) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandling.getId(), !behandling.erBehandlingAvSøknad());
        } else if (behandlingsresultat.erInnvilgelse()) {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandling.getId(), false);
        } else if (lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.AVSLAATT) {
            log.debug("Behandling {}: MEDL oppdateres ikke i forbindelse med avslag.", behandling.getId());
        } else {
            throw new FunksjonellException("Opprettelse av Periode i MEDL støttes ikke for behandlingsresultat type "
                + behandlingsresultat.getType() + " og InnvilgelsesResultat type " + lovvalgsperiode.getInnvilgelsesresultat().getKode());
        }
    }

    private void oppdaterEksisterendeMedlPeriode(Lovvalgsperiode lovvalgsperiode) throws FunksjonellException, TekniskException {
        if (lovvalgsperiode.erInnvilget()) {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false);
        } else if (lovvalgsperiode.erAvslått()) {
            medlPeriodeService.avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
        } else {
            throw new FunksjonellException("Kan ikke oppdatere medlperiode: Lovvalgsperiode med ID " + lovvalgsperiode.getId() +
                " har en medlperiodeID, men er verken innvilget eller avslått.");
        }
    }
}
