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

import java.time.LocalDate;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BRUKER_ID;

/**
 * Steget sørger for å hente arbeidsforholdinfo fra AAREG
 *
 * Transisjoner:
 * HENT_ARBF_OPPL → HENT_INNT_OPPL hvis alt ok
 * HENT_ARBF_OPPL → FEILET_MASKINELT hvis oppslag mot AAREG feilet
 */
@Component
public class HentArbeidsforholdopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentArbeidsforholdopplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public HentArbeidsforholdopplysninger(RegisteropplysningerService registeropplysningerService) {
        this.registeropplysningerService = registeropplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ARBF_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        LocalDate tom = periode.getTom() == null ? periode.getFom().plusYears(1) : periode.getTom();

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .fnr(brukerId)
                .fom(periode.getFom())
                .tom(tom)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .arbeidsforholdopplysninger().build())
                .build());

        prosessinstans.setSteg(ProsessSteg.HENT_INNT_OPPL);
        log.info("Hentet arbeidsforholdopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
