package no.nav.melosys.regler.motor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tilbyr forskjellige metoder for logging
 * 
 * Metodene som tilbys av denne klassen skal prefikse alle loggmeldingene med regelnavn.
 * 
 */
public class RegelLogg {

    private static Logger log = LoggerFactory.getLogger(RegelLogg.class); // Logger til FastsettLovvalg sin kanal

    public static void loggInfo(String format, Object... arguments) {
        if (log.isInfoEnabled()) {
            String regelnavn = finnRegelNavn();
            log.info(regelnavn + ": " + format, arguments);
        }
    }

    public static void loggError(String format, Object... arguments) {
        String regelnavn = finnRegelNavn();
        log.error(regelnavn + ": " + format, arguments);
    }

    /* Regelnavnet som skal logges er "klassenavn.metode" der regelen er implementert */
    private static String finnRegelNavn() {
        for (StackTraceElement se : Thread.currentThread().getStackTrace()) {
            String regelKlasse = se.getClassName();
            if (!regelKlasse.startsWith("no.nav.melosys.regler.")) continue; // Eliminerer treff på evt. proxys
            regelKlasse = regelKlasse.replaceAll("no.nav.melosys.regler.", ""); // Fjerner felles pakke-prefiks fra regelnavn
            if (regelKlasse.startsWith("motor.")) continue; // For å eliminere interne kall i regelrammeverk
            if (regelKlasse.indexOf('.') == regelKlasse.lastIndexOf('.')) continue; // Bla videre opp hvis hovedpakken til en regeltjeneste
            // Vi er her bare hvis vi har bladd oss helt opp til en regelimplementasjon i stack trace 
            String regelMetode = se.getMethodName();
            return  regelKlasse + "." + regelMetode;
        }
        // Er vi her betyr det at vi kaller denne klassen fra utsiden av no.nav.melosys.regler.lovvalg 
        throw new RuntimeException("Intern feil: RegelLogg (eller en annen klasse i samme pakke) skal ikke kalles fra andre pakker enn no.nav.melosys.regler.*");
    }

}
