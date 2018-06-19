package no.nav.melosys.saksflyt.agent.unntak;

import java.time.Duration;
import java.time.Instant;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.saksflyt.agent.unntak.SettTilFeilet.settTilFeilet;

public class Retry implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(Retry.class);

    private int maxAntallForsøk;
    
    private long millisMellomForsøk;
    
    private Retry() {
    }
    
    public static Retry prøvIgjen(int maxAntallForsøk, long millisMellomForsøk) {
        Retry res = new Retry();
        res.maxAntallForsøk = maxAntallForsøk;
        res.millisMellomForsøk = millisMellomForsøk;
        return res;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, Throwable t) {
        int antForsøk = prosessinstans.getAntallRetry() + 1;
        logger.debug("Prosessinstans {} feilet for {}de gang", prosessinstans.getId(), antForsøk);
        prosessinstans.setAntallRetry(antForsøk);
        if (antForsøk >= maxAntallForsøk) {
            settTilFeilet().behandleUnntak(prosessinstans, t);
            return;
        }
        Instant skalSoveTil = Instant.now().plus(Duration.ofMillis(millisMellomForsøk));
        prosessinstans.setSoverTil(skalSoveTil);
        return;
    }

}
