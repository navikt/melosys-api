package no.nav.melosys.saksflyt.steg.unntak;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
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
        logger.warn("Setter prosessinstans {} til feilet.", prosessinstans.getId());
        prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
    }

}
