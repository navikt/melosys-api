package no.nav.melosys.regler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegelLoggerFactory {
    
    /**
     * Returnerer riktig Logger for en regelklasse 
     */
    public static Logger getRegelLogger() {
        String regelKlasse = Thread.currentThread().getStackTrace()[2].getClassName();
        String regelNavn = regelKlasse.replaceAll("no.nav.melosys.regler.", "");
        return LoggerFactory.getLogger(regelNavn);
    }

}
