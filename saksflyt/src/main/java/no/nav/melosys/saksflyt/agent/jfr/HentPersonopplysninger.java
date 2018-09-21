package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;
import static no.nav.melosys.domain.ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;

/**
 * Steget sørger for å hente personinfo fra TPS
 * 
 * Transisjoner: 
 * JFR_HENT_PERS_OPPL → JFR_VURDER_INNGANGSVILKÅR hvis alt ok
 * JFR_HENT_PERS_OPPL → FEILET_MASKINELT hvis personen ikke finnes i TPS
 */
@Component
public class HentPersonopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentPersonopplysninger.class);

    private final SaksopplysningRepository saksopplysningRepo;

    private final TpsFasade tpsFasade;

    @Autowired
    public HentPersonopplysninger(SaksopplysningRepository saksopplysningRepo, @Qualifier("system")TpsFasade tpsFasade) {
        this.saksopplysningRepo = saksopplysningRepo;
        this.tpsFasade = tpsFasade;
        log.info("HentPersonopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_HENT_PERS_OPPL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String brukerId = prosessinstans.getData(BRUKER_ID);
        Periode søknadsperiode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        Behandling behandling = prosessinstans.getBehandling();

        Saksopplysning saksopplysning = tpsFasade.hentPersonMedAdresse(brukerId);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(LocalDateTime.now());
        saksopplysningRepo.save(saksopplysning);

        saksopplysning = tpsFasade.hentPersonhistorikk(brukerId, søknadsperiode.getFom());
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(LocalDateTime.now());
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(JFR_VURDER_INNGANGSVILKÅR);
        log.info("Hentet personopplysninger for prosessinstans {}", prosessinstans.getId());
    }
}
