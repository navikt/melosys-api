package no.nav.melosys.saksflyt.steg.reg;

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

/**
 * Steget sørger for å hente Opplysninger om Orgnisjoner fra EREG
 *
 * Transisjoner:
 * HENT_ORG_OPP → HENT_MEDL_OPPL hvis alt ok
 */
@Component
public class HentOrganisasjonsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentOrganisasjonsopplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public HentOrganisasjonsopplysninger(RegisteropplysningerService registeropplysningerService) {
        this.registeropplysningerService = registeropplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ORG_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .organisasjonsopplysninger().build())
                .build());

        prosessinstans.setSteg(ProsessSteg.HENT_MEDL_OPPL);
        log.info("Hentet organisasjonsopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
