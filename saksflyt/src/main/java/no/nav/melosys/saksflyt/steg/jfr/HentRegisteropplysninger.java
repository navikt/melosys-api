package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.dokument.felles.Periode;
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

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

/**
 * Steget sørger for å hente personinfo fra TPS
 *
 * Transisjoner:
 * JFR_HENT_PERS_OPPL → JFR_VURDER_INNGANGSVILKÅR hvis alt ok
 * JFR_HENT_PERS_OPPL → FEILET_MASKINELT hvis personen ikke finnes i TPS
 */
@Component("JFRHentRegisteropplysninger")
public class HentRegisteropplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentRegisteropplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public HentRegisteropplysninger(RegisteropplysningerService registeropplysningerService) {
        this.registeropplysningerService = registeropplysningerService;
        log.info("HentPersonopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_HENT_REGISTER_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode søknadsperiode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        // FIXME: Logikk i registeropplysningerService for å sette default tom dato

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .fnr(brukerId)
                .fom(søknadsperiode.getFom())
                .tom(søknadsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .organisasjonsopplysninger()
                    .medlemskapsopplysninger()
                    .sakOgBehandlingopplysninger()
                    .build())
                .build());

        prosessinstans.setSteg(JFR_VURDER_INNGANGSVILKÅR);

        log.info("Hentet personopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
