package no.nav.melosys.saksflyt.agent.reg;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, TekniskException, SikkerhetsbegrensningException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        LocalDate fom = periode.getFom().minusMonths(arbeidsforholdhistorikkAntallMåneder);

        LocalDate tom;
        if (periode.getTom().isAfter(LocalDate.now())) {
            tom = LocalDate.now();
        } else {
            tom = periode.getTom();
        }

        Instant nå = Instant.now();
        Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(brukerId, AaregFasade.REGELVERK_A_ORDNINGEN, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(ProsessSteg.HENT_INNT_OPPL);
        log.info("Hentet arbeidsforholdopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
