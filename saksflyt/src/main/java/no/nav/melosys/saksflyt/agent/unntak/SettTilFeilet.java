package no.nav.melosys.saksflyt.agent.unntak;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettTilFeilet implements UnntakBehandler {

    private static Logger logger = LoggerFactory.getLogger(SettTilFeilet.class);

    private static SettTilFeilet instanse = new SettTilFeilet();
    
    private SettTilFeilet() {
    }

    public static SettTilFeilet settTilFeilet() {
        return instanse;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t) {
        logger.error("Settes prosessinstans {} til feilet: {}", prosessinstans.getId(), melding, t);
        prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
    }

}
