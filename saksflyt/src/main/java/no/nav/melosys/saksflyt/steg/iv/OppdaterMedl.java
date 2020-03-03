package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_OPPDATER_MEDL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_SEND_BREV;

/**
 * Oppdaterer medlemskap periode i MEDL.
 *
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK og ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE
 *  IV_OPPDATER_MEDL -> IV_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final OppdaterMedlFelles felles;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, OppdaterMedlFelles felles) {
        this.medlFasade = medlFasade;
        this.felles = felles;
        log.info("IverksetteVedtakOppdaterMEDL initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_OPPDATER_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        String fnr = felles.hentFnr(behandling);
        Lovvalgsperiode lovvalgsperiode = felles.hentLovvalgsperiode(behandling);

        Behandlingsresultat behandlingsresultat = felles.hentBehandlingsresultat(behandling);
        if (lovvalgsperiode.getMedlPeriodeID() != null) {
            oppdaterEksisterendeMedlPeriode(lovvalgsperiode);
        } else if (behandlingsresultat.erInnvilgelseFlereLand()) {
            KildedokumenttypeMedl kildedokumenttypeMedl = behandling.harSøknad() ? KildedokumenttypeMedl.HENV_SOKNAD : KildedokumenttypeMedl.SED;
            Long medlPeriodeID = medlFasade.opprettPeriodeForeløpig(fnr, lovvalgsperiode, kildedokumenttypeMedl);
            felles.lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandling.getId());
        } else if (behandlingsresultat.erInnvilgelse()) {
            Long medlPeriodeID = medlFasade.opprettPeriodeEndelig(fnr, lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
            felles.lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandling.getId());
        } else if (lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.AVSLAATT) {
            log.debug("Behandling {}: MEDL oppdateres ikke i forbindelse med avslag.", behandling.getId());
        } else {
            throw new FunksjonellException("Opprettelse av Periode i MEDL støttes ikke for behandlingsresultat type "
                + behandlingsresultat.getType() + " og InnvilgelsesResultat type " + lovvalgsperiode.getInnvilgelsesresultat().getKode());
        }

        prosessinstans.setSteg(IV_SEND_BREV);
    }

    private void oppdaterEksisterendeMedlPeriode(Lovvalgsperiode lovvalgsperiode) throws FunksjonellException, TekniskException {
        if (lovvalgsperiode.erInnvilget()) {
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        } else if (lovvalgsperiode.erAvslått()) {
            medlFasade.avvisPeriode(lovvalgsperiode.getMedlPeriodeID(), StatusaarsakMedl.AVVIST);
        } else {
            throw new FunksjonellException("Kan ikke oppdatere medlperiode: Lovvalgsperiode med ID " + lovvalgsperiode.getId() +
                " har en medlperiodeID, men er verken innvilget eller avslått.");
        }
    }
}
