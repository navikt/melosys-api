package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDateTime;
import java.time.YearMonth;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;

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

    @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}")
    private Integer inntektshistorikkAntallMåneder;

    @Autowired
    public HentInntektopplysninger(InntektFasade inntektFasade, SaksopplysningRepository saksopplysningRepo) {
        this.inntektFasade = inntektFasade;
        this.saksopplysningRepo = saksopplysningRepo;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_INNT_OPPL;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        YearMonth fom = YearMonth.from(periode.getFom()).minusMonths(inntektshistorikkAntallMåneder);
        YearMonth tom = YearMonth.now();

        try {
            Behandling behandling = prosessinstans.getBehandling();
            Saksopplysning saksopplysning = inntektFasade.hentInntektListe(brukerId, fom, tom);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setRegistrertDato(LocalDateTime.now());
            saksopplysningRepo.save(saksopplysning);

        } catch (IntegrasjonException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
            return;
        }

        prosessinstans.setSteg(ProsessSteg.HENT_ORG_OPPL);
    }
}
