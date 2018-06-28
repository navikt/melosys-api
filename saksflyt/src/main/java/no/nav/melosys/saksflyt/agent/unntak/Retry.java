package no.nav.melosys.saksflyt.agent.unntak;

import java.time.Duration;
import java.time.Instant;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Retry implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(Retry.class);

    private int maxAntallForsøk;
    
    private long millisMellomForsøk;
    
    UnntakBehandler feiletBehandler; // Brukes når retry-taket er nådd
    
    private Retry() {
    }
    
    public static Retry prøvIgjen(int maxAntallForsøk, long millisMellomForsøk) {
        Retry res = new Retry();
        res.maxAntallForsøk = maxAntallForsøk;
        res.millisMellomForsøk = millisMellomForsøk;
        res.feiletBehandler = SettTilFeilet.settTilFeilet();
        return res;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t) {
        int antForsøk = prosessinstans.getAntallRetry() + 1;
        logger.debug("Prosessinstans {} feilet for {}-te gang: {}", prosessinstans.getId(), antForsøk, melding, t);
        prosessinstans.setAntallRetry(antForsøk);
        if (antForsøk >= maxAntallForsøk) {
            feiletBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        Instant skalSoveTil = Instant.now().plus(Duration.ofMillis(millisMellomForsøk));
        prosessinstans.setSoverTil(skalSoveTil);
        return;
    }

}
