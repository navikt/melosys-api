package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
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

    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallÅr}")
    private Integer arbeidsforholdhistorikkAntallÅr;

    private final AaregFasade aaregFasade;

    private final SaksopplysningRepository saksopplysningRepo;

    @Autowired
    public HentArbeidsforholdopplysninger(AaregFasade aaregFasade, SaksopplysningRepository saksopplysningRepo) {
        this.aaregFasade = aaregFasade;
        this.saksopplysningRepo = saksopplysningRepo;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ARBF_OPPL;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        LocalDate fom = periode.getFom().minusYears(arbeidsforholdhistorikkAntallÅr);
        LocalDate tom = LocalDate.now();

        try {
            Behandling behandling = prosessinstans.getBehandling();
            Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(brukerId, AaregFasade.REGELVERK_A_ORDNINGEN, fom, tom);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setRegistrertDato(LocalDateTime.now());
            saksopplysningRepo.save(saksopplysning);

        } catch (IntegrasjonException | TekniskException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
            return;
        }

        prosessinstans.setSteg(ProsessSteg.HENT_INNT_OPPL);
    }
}
