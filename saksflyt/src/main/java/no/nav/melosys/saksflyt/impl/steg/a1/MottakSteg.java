package no.nav.melosys.saksflyt.impl.steg.a1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.Steg;
import no.nav.melosys.service.MottakService;

/**
 * Steget sørger for å: * Lese en melding fra JMS-køen * Hente søknaden svarende til meldingen * Opprette en Fagsak og
 * Behandling i databasen
 */
@Component
public class MottakSteg implements Steg {

    private static final Logger log = LoggerFactory.getLogger(MottakSteg.class);

    @Autowired
    private Binge binge;

    @Autowired
    private MottakService mottakService;

    boolean ready = false;

    @Override
    public void finnBehandlingOgUtfoerSteg() {

        // TODO JMS
        if (ready) {
            return;
        }

        // Opprette en Fagsak og Behandling i databasen
        String fnr = "FJERNET"; // FIXME: Mottak

        try {
            Behandling behandling = mottakService.opprettSak(fnr);
            binge.leggTil(behandling);
        } catch (Throwable t) {
            log.error("Feil ", t.getCause()); // TODO Exceptions i Melosys
        }

    }
}
