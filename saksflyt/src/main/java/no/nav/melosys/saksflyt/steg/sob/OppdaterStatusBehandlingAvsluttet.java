package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterStatusBehandlingAvsluttet implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingAvsluttet.class);

    private final SobService sobService;

    @Autowired
    public OppdaterStatusBehandlingAvsluttet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SOB_BEHANDLING_AVSLUTTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingAvsluttet(prosessinstans.getBehandling().getId());
        log.info("Oppdatert sob-status til avsluttet for prosessinstans {}", prosessinstans.getId());
    }
}
