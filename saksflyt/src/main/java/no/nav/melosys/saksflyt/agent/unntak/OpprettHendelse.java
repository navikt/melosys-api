package no.nav.melosys.saksflyt.agent.unntak;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpprettHendelse implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(OpprettHendelse.class);

    private String type;
    private String melding;
    
    private OpprettHendelse() {
    }
    
    public static OpprettHendelse opprettHendelse(String type, String melding) {
        OpprettHendelse res = new OpprettHendelse();
        res.type = type;
        res.melding = melding;
        return res;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, Throwable t) {
        logger.info("Legger på hendelse på {}: {}", prosessinstans.getId(), melding);
        prosessinstans.leggTilHendelse(type, melding);
    }

}
