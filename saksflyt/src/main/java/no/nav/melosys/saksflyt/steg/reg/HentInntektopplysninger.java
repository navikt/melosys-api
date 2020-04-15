package no.nav.melosys.saksflyt.steg.reg;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BRUKER_ID;

/**
 * Steget sørger for å hente inntektinfo fra INNTK
 *
 * Transisjoner:
 * HENT_INNT_OPPL → HENT_ORG_OPPL hvis alt ok
 * HENT_INNT_OPPL → FEILET_MASKINELT hvis oppslag mot INNTK feilet
 */
@Component
public class HentInntektopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentInntektopplysninger.class);

    private final InntektFasade inntektFasade;
    private final SaksopplysningRepository saksopplysningRepo;
    private final Integer inntektshistorikkAntallMåneder;

    @Autowired
    public HentInntektopplysninger(InntektFasade inntektFasade,
                                   SaksopplysningRepository saksopplysningRepo,
                                   @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}") Integer inntektshistorikkAntallMåneder) {
        this.inntektFasade = inntektFasade;
        this.saksopplysningRepo = saksopplysningRepo;
        this.inntektshistorikkAntallMåneder = inntektshistorikkAntallMåneder;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_INNT_OPPL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, SikkerhetsbegrensningException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        final LocalDate iDag = LocalDate.now();
        YearMonth fom;
        if (periode.getFom().isAfter(iDag)) {
            fom = YearMonth.from(iDag).minusMonths(inntektshistorikkAntallMåneder);
        } else {
            fom = YearMonth.from(periode.getFom()).minusMonths(inntektshistorikkAntallMåneder);
        }

        YearMonth tom;
        if (periode.getTom().isAfter(iDag)) {
            tom = YearMonth.from(iDag);
        } else {
            tom = YearMonth.from(periode.getTom());
        }

        final Instant nå = Instant.now();
        Behandling behandling = prosessinstans.getBehandling();
        log.info("Henter inntektopplysninger for behandling {} og periode {} til {}", behandling, fom, tom);
        Saksopplysning saksopplysning = inntektFasade.hentInntektListe(brukerId, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        try {
            saksopplysningRepo.save(saksopplysning);
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause != null) {
                log.error(rootCause.getMessage(), rootCause);
            }
            throw e;
        }

        prosessinstans.setSteg(ProsessSteg.HENT_ORG_OPPL);
    }
}
