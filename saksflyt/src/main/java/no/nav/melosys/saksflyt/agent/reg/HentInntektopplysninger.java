package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
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
        log.info("HentInntektopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_INNT_OPPL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, SikkerhetsbegrensningException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        YearMonth fom = YearMonth.from(periode.getFom()).minusMonths(inntektshistorikkAntallMåneder);
        YearMonth tom = YearMonth.now();

        Behandling behandling = prosessinstans.getBehandling();
        Saksopplysning saksopplysning = inntektFasade.hentInntektListe(brukerId, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(LocalDateTime.now());
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(ProsessSteg.HENT_ORG_OPPL);
        log.info("Hentet inntektopplysninger for {}", prosessinstans.getId());
    }
}
