package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_MEDL_OPPL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;

/**
 * Steget sørger for å hente medlemskapsinfo fra MEDL
 *
 * Transisjoner:
 * HENT_MEDL_OPPL → HENT_SOB_SAKER hvis alt ok
 * HENT_MEDL_OPPL → FEILET_MASKINELT hvis oppslag mot MEDL feilet
 */
@Component
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public HentMedlemskapsopplysninger(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
        log.info("HentMedlemskapsopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_MEDL_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        saksopplysningerService.hentSaksopplysningMedl(prosessinstans.getBehandling().getId(), periode.getFom(), periode.getTom());
        prosessinstans.setSteg(HENT_SOB_SAKER);
        log.info("Hentet medlemskapsopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
