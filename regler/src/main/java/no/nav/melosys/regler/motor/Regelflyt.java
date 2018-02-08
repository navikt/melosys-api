package no.nav.melosys.regler.motor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.melosys.regler.motor.Regelpakke.Regel;

/**
 * Støtte for en sekvensiell regelflyt.
 * 
 * En regelflyt inneholder regelpakker som eksekveres sekvensielt. 
 * 
 * Det er inntil videre ikke behov for komplekse regelflyt (som f.eks. inneholder forgreninger eller løkker).
 * 
 */
public class Regelflyt {
    
    private static final Logger log = LoggerFactory.getLogger(Regelflyt.class);
    
    /** Regelpakkene som skal kjøres i sekvens */
    private List<Class<? extends Regelpakke>> flytSekvens = new ArrayList<>();
    
    public Regelflyt() {
    }
    
    /** kjører alle regelpakkene i flyten */
    public void kjør() {
        try {
            for (Class<? extends Regelpakke> regelpakke : flytSekvens) {
                kjørRegelPakke(regelpakke);
            }
        } catch (AvbrytRegelkjoeringIStillhetException e) {
            // En av reglene har avbrutt videre regelkjøring. Returner i stillhet.
                return;
        }
    }

    /** Legger til en eller flere regelpakket til flyten. */
    @SafeVarargs // OK at vararg blir upcastet til Object[]
    protected final void leggTilRegelpakker(Class<? extends Regelpakke>... regelpakker) {
        Collections.addAll(flytSekvens, regelpakker);
    }
 
    /* Kjører alle klassens static metoder som er annotert som Regel i tilfeldig rekkefølge */
    private static final void kjørRegelPakke(Class<? extends Regelpakke> regelpakke) {
        log.debug("Kjører alle reglene i pakke {}...", regelpakke.getSimpleName());
        for (Method regelKandidat : regelpakke.getDeclaredMethods()) {
            if (regelKandidat.isAnnotationPresent(Regel.class)) {
                try {
                    regelKandidat.invoke(null);
                } catch (InvocationTargetException e) {
                    // Vi er her hvis regelmetoden kaster exception
                    if (e.getCause() instanceof AvbrytRegelkjoeringIStillhetException) {
                        // En av reglene har avbrutt videre regelkjøring
                        throw (AvbrytRegelkjoeringIStillhetException) e.getCause();
                    }
                    String feilmelding = "Ubehandlet exception ved kjøring av regel " + regelpakke.getSimpleName() + "." + regelKandidat.getName();
                    log.error(feilmelding, e.getCause());
                    throw new RuntimeException(feilmelding, e.getCause());
                } catch (IllegalAccessException | IllegalArgumentException | NullPointerException e) {
                    // Vi er her enten hvis metoden ikke er static, eller hvis initialiseringen av klassen kastet exception, eller hvis metoden har parametere
                    String feilmelding = "Teknisk feil i implementasjonen av regel " + regelpakke.getSimpleName() + "." + regelKandidat.getName();
                    log.error(feilmelding, e);
                    throw new RuntimeException(feilmelding, e);
                }
            }
        }
    }

}
