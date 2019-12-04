package no.nav.melosys.saksflyt.steg.sob;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;

/**
 * Steget sørger for å hente saker fra SOB
 *
 * Transisjoner:
 * HENT_SOB_SAKER → OPPFRISK_SAKSOPPLYSNINGER hvis alt ok
 * HENT_SOB_SAKER → FEILET_MASKINELT hvis oppslag mot SOB feilet
 */
@Component
public class HentSakOgBehandlingSaker extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentSakOgBehandlingSaker.class);

    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    private final SaksopplysningRepository saksopplysningRepo;

    @Autowired
    public HentSakOgBehandlingSaker(SakOgBehandlingFasade sakOgBehandlingFasade, SaksopplysningRepository saksopplysningRepo) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.saksopplysningRepo = saksopplysningRepo;
        log.info("HentSakOgBehandlingSaker initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HENT_SOB_SAKER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws IntegrasjonException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);

        Instant nå = Instant.now();
        Behandling behandling = prosessinstans.getBehandling();
        Saksopplysning saksopplysning = sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørId);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepo.save(saksopplysning);

        prosessinstans.setSteg(OPPFRISK_SAKSOPPLYSNINGER);
        log.info("Hentet saker fra Sak og behandling for prosessinstans {}", prosessinstans.getId());
    }
}
