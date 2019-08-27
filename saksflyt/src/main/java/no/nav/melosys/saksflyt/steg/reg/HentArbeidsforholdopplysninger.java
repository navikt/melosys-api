package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;

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

    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}")
    private Integer arbeidsforholdhistorikkAntallMåneder;

    private final AaregFasade aaregFasade;

    private final SaksopplysningRepository saksopplysningRepo;

    @Autowired
    public HentArbeidsforholdopplysninger(AaregFasade aaregFasade, SaksopplysningRepository saksopplysningRepo) {
        this.aaregFasade = aaregFasade;
        this.saksopplysningRepo = saksopplysningRepo;
        log.info("HentArbeidsforholdopplysninger initialisert");

    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ARBF_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, SikkerhetsbegrensningException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        final LocalDate iDag = LocalDate.now();
        LocalDate fom;
        if (periode.getFom().isAfter(iDag)) {
            fom = iDag.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        } else {
            fom = periode.getFom().minusMonths(arbeidsforholdhistorikkAntallMåneder);
        }

        LocalDate tom;
        if (periode.getTom() == null || periode.getTom().isAfter(iDag)) {
            tom = iDag;
        } else {
            tom = periode.getTom();
        }

        final Instant nå = Instant.now();
        Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(brukerId, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(ProsessSteg.HENT_INNT_OPPL);
        log.info("Hentet arbeidsforholdopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
