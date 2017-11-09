package no.nav.melosys.regler.motor;

import static no.nav.melosys.regler.motor.RegelLogg.loggError;
import static no.nav.melosys.regler.motor.RegelLogg.loggInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Kontekst (og -manager) for en regelkjøring.
 * 
 * Konteksten er bundet til tråden den kjører på, slik at regelsett kan kalles i parallell.
 * 
 */
public class KontekstManager {

    private static ThreadLocal<Map<Object, Object>> lokaleVerdier = new ThreadLocal<>();

    /** Initialiserer regelkjøringens kontekst. Må gjøres før man oppretter eller kjører regler. */
    public static void initialiserLokalKontekst() {
        if (lokaleVerdier.get() != null) {
            throw new RuntimeException("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
        }
        lokaleVerdier.set(new HashMap<>());
    }
    
    /** Sletter regelkjøringens kontekst. Må gjøres etter at alle regelsettene er kjørt. */
    public static void slettLokalKontekst() {
        lokaleVerdier.set(null);
    }

    public static void settVariabel(Object variabel, Object verdi) {
        loggInfo("Setter '{}' til {}", variabel, verdi.toString());
        lokaleVerdier.get().put(variabel, verdi);
    }

    public static Object hentVariabel(Object variabel) {
        if (!lokaleVerdier.get().containsKey(variabel)) {
            // FIXME: Hva skal oppførsel være her? Logge? Stillhet? Exception?
            loggError("Forsøk på å hente variabelen '{}' før den er satt");
        }
        return lokaleVerdier.get().get(variabel);
    }

}
