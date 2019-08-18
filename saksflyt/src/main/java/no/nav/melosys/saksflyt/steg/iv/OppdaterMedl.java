package no.nav.melosys.saksflyt.steg.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;
import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;

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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        String fnr = felles.hentFnr(behandling);
        Lovvalgsperiode lovvalgsperiode = felles.hentLovvalgsperiode(behandling);

        Behandlingsresultat behandlingsresultat = felles.hentBehandlingsresultat(behandling);
        if (lovvalgsperiode.getMedlPeriodeID() != null) {
            medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        } else if (erPeriodeEndelig(behandlingsresultat, lovvalgsperiode)) {
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

    boolean erPeriodeEndelig(Behandlingsresultat behandlingsresultat, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultat.getType() == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET;
    }
}
