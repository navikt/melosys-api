package no.nav.melosys.saksflyt.steg.unntak;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpprettHendelse implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(OpprettHendelse.class);

    private String type;
    
    private OpprettHendelse() {
    }
    
    static OpprettHendelse opprettHendelse(String type) {
        OpprettHendelse res = new OpprettHendelse();
        res.type = type;
        return res;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t) {
        logger.debug("Legger på hendelse på {}: {}", prosessinstans.getId(), melding);
        prosessinstans.leggTilHendelse(type, melding, t);
    }

}
