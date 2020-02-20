package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_MEDL_OPPL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;

/**
 * Steget sørger for å hente medlemskapsinfo fra MEDL
 * <p>
 * Transisjoner:
 * HENT_MEDL_OPPL → HENT_SOB_SAKER hvis alt ok
 * HENT_MEDL_OPPL → FEILET_MASKINELT hvis oppslag mot MEDL feilet
 */
@Component
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public HentMedlemskapsopplysninger(RegisteropplysningerService registeropplysningerService) {
        this.registeropplysningerService = registeropplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_MEDL_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .fom(periode.getFom())
                .tom(periode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());

        prosessinstans.setSteg(HENT_SOB_SAKER);
        log.info("Hentet medlemskapsopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
