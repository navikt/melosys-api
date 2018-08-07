package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
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
import static no.nav.melosys.domain.ProsessSteg.HENT_MEDL_OPPL;
import static no.nav.melosys.domain.ProsessSteg.HENT_SOB_SAKER;

/**
 * Steget sørger for å hente medlemskapsinfo fra MEDL
 *
 * Transisjoner:
 * HENT_MEDL_OPPL → OPPRETT_OPPGAVE hvis alt ok
 * HENT_MEDL_OPPL → FEILET_MASKINELT hvis oppslag mot MEDL feilet
 */
@Component
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final MedlFasade medlFasade;

    private final SaksopplysningRepository saksopplysningRepo;

    private final Integer medlemskaphistorikkAntallÅr;

    @Autowired
    public HentMedlemskapsopplysninger(MedlFasade medlFasade, SaksopplysningRepository saksopplysningRepo,
                                       @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr) {
        this.medlFasade = medlFasade;
        this.saksopplysningRepo = saksopplysningRepo;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
        log.info("HentMedlemskapsopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_MEDL_OPPL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, SikkerhetsbegrensningException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert
        LocalDate fom = periode.getFom().minusYears(medlemskaphistorikkAntallÅr);
        LocalDate tom = LocalDate.now();

        Behandling behandling = prosessinstans.getBehandling();
        Saksopplysning saksopplysning = medlFasade.hentPeriodeListe(brukerId, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(LocalDateTime.now());
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(HENT_SOB_SAKER);
        log.info("Hentet medlemskapsopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
