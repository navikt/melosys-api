package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.SOB_BEHANDLING_OPPRETTET;

@Component
public class OppdaterStatusBehandlingOpprettet implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingOpprettet.class);

    private final SobService sobService;

    public OppdaterStatusBehandlingOpprettet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return SOB_BEHANDLING_OPPRETTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        sobService.sakOgBehandlingOpprettet(prosessinstans.getBehandling().getId());
        log.info("Oppdatert sob-status til opprettet for behandling {}", behandlingID);
    }
}
