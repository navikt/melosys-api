package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_MEDL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_SEND_BREV;


/**
 * Oppdaterer medlemskap periode i MEDL.
 *
 * Transisjoner:
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_OPPDATER_MEDL -> AOU_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final MedlPeriodeService felles;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, MedlPeriodeService felles) {
        this.felles = felles;
        this.medlFasade = medlFasade;
        log.info("AnmodningOmUnntakOppdaterMEDL initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_OPPDATER_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        String fnr = felles.hentFnr(behandling);
        Anmodningsperiode anmodningsperiode = felles.hentAnmodningsperiode(behandling);
        Long medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, anmodningsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
        felles.lagreMedlPeriodeId(medlPeriodeID, anmodningsperiode, behandling.getId());

        prosessinstans.setSteg(AOU_SEND_BREV);
    }
}
