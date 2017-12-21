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

    private static final ThreadLocal<Map<Object, Object>> lokaleVerdier = new ThreadLocal<>();

    /** Initialiserer regelkjøringens kontekst. Må gjøres før man oppretter eller kjører regler. */
    public static final void initialiserLokalKontekst() {
        if (lokaleVerdier.get() != null) {
            throw new RuntimeException("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
        }
        lokaleVerdier.set(new HashMap<>());
    }
    
    /** Sletter regelkjøringens kontekst. Må gjøres etter at alle regelsettene er kjørt. */
    public static final void slettLokalKontekst() {
        lokaleVerdier.set(null);
    }

    public static final void settVariabel(Object variabel, Object verdi) {
        loggInfo("Setter '{}' til {}", variabel, verdi.toString());
        lokaleVerdier.get().put(variabel, verdi);
    }

    public static final Object hentVariabel(Object variabel) {
        if (!lokaleVerdier.get().containsKey(variabel)) {
            loggError("Forsøk på å hente variabelen '{}' før den er satt", variabel);
            throw new RuntimeException();
        }
        return lokaleVerdier.get().get(variabel);
    }

}
