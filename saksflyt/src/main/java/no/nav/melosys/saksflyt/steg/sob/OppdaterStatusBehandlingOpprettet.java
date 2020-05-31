package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 */
@Component
public class OppdaterStatusBehandlingOpprettet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingOpprettet.class);

    private final SobService sobService;

    @Autowired
    public OppdaterStatusBehandlingOpprettet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return STATUS_BEH_OPPR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørID = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Behandling behandling = prosessinstans.getBehandling();

        sobService.sakOgBehandlingOpprettet(saksnummer, behandling.getId(), aktørID);

        if (prosessinstans.getType() == ProsessType.OPPRETT_NY_SAK) {
            prosessinstans.setSteg(JFR_HENT_REGISTER_OPPL);
        } else {
            prosessinstans.setSteg(JFR_OPPDATER_SAKSRELASJON);
        }
        log.info("Oppdatert sob-status til opprettet for prosessinstans {}", prosessinstans.getId());
    }
}
