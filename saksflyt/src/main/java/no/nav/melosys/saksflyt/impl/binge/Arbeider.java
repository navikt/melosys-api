package no.nav.melosys.saksflyt.impl.binge;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import no.nav.melosys.saksflyt.api.Steg;

/**
 * Komponent med arbeidertråder som schedulerer arbeid som utføres av de maskinelle stegene.
 * 
 * Dette er en passe dum implementasjon, der x tråder hver for seg looper gjennom alle behandlingssteg og aktiverer dem. Dette
 * gjentas i det uendelige.
 * 
 * Konfigurasjon: melosys.saksflyt.arbeider.antallTråder – Antall tråder melosys.saksflyt.arbeider.oppholdMellomLooping – Hvor
 * mange millisekunder trådene skal sove mellom hver løkke melosys.saksflyt.arbeider.oppholdMellomSteg – Hvor mange
 * millisekunder trådene skal sove mellom hvert steg som aktiveres
 * 
 */
@Component
@Scope("singleton")
public class Arbeider {

    private static final int ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER = 20;
    private static final int OPPHOLD_MELLOM_RETRY_FOR_Å_STOPPE_TRÅDER = 10000;
    private static final long TIMEOUT_FOR_Å_STOPPE_EN_TRÅD = 60000;

    private static final Logger logger = LoggerFactory.getLogger(Arbeider.class);

    /** Antall arbeidstråder */
    @Value("${melosys.saksflyt.arbeider.antallTraader}")
    private int antallTråder;

    /** Opphold mellom hvert steg */
    @Value("${melosys.saksflyt.arbeider.oppholdMellomSteg}")
    private long oppholdMellomSteg;

    @Autowired
    private List<Steg> maskinelleSteg;

    private ArbeiderTraad[] tråder;

    private void loggSteg() {
        if (!logger.isInfoEnabled()) {
            return;
        }
        List<String> klasser = new ArrayList<>();
        for (Steg steg : maskinelleSteg) {
            Class<?> c = steg.getClass();
            for (;;) { // Iterer superklasser helt til vi ikke har en proxy
                if (c.getName().startsWith("no.nav") || c.getName().startsWith("java.")) {
                    break;
                }
                c = c.getSuperclass();
            }
            klasser.add(c.getName());
        }
        klasser.sort(String.CASE_INSENSITIVE_ORDER);
        logger.info("Steg som aktiveres av arbeideren:");
        klasser.stream().forEach(logger::info);
    }

    /**
     * Starter prosessering. Skal kun kalles av spring etter at alt er injisert.
     */
    @PostConstruct
    public void start() {
        loggSteg();
        tråder = new ArbeiderTraad[antallTråder];
        for (int i = 0; i < antallTråder; i++) {
            tråder[i] = new ArbeiderTraad();
            tråder[i].start();
        }
    }

    /**
     * Stopper prosessering. Skal kun kalles av spring når konteksten skal tas ned.
     */
    @PreDestroy
    public void stopp() {
        for (int forsøk = 1; forsøk <= ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER; forsøk++) { // Ytre løkke for retry på å stoppe alle
                                                                                      // tråder
            logger.info("Forsøk nr. {} på å stoppe alle arbeidertråder...", forsøk);
            // Forsøk å stoppe alle trådene...
            // Dette gjøres nå i sekvens, noe som er helt greit hvis vi ikke har for mange tråder å stoppe.
            for (ArbeiderTraad t : tråder) {
                t.stoppArbeider();
            }
            boolean alleTråderStoppet = true;
            for (ArbeiderTraad t : tråder) {
                if (t.isAlive()) {
                    logger.error("Kunne ikke stoppe alle arbeidertrådene");
                    alleTråderStoppet = false;
                    break;
                }
            }
            if (alleTråderStoppet) {
                logger.info("Alle arbeidertråder er stoppet");
                return;
            }
            // Et lite opphold før neste forsøk...
            try {
                Thread.sleep(OPPHOLD_MELLOM_RETRY_FOR_Å_STOPPE_TRÅDER);
            } catch (InterruptedException e) {
                // Ok. Skjer ikke. Og ingen problem hvis det skjer heller.
            }
        }
        // Vi er her bare hvis vi ikke klarte å stoppe alle trådene
        // Alt vi kan gjøre nå er å logge problemet
        logger.error("KRITISK FEIL: Ga opp å stoppe alle arbeidertråder etter {} forsøk", ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER);
    }

    private class ArbeiderTraad extends Thread {
        private volatile boolean skalStoppe = false;

        public void stoppArbeider() {
            if (!isAlive())
                return;
            skalStoppe = true;
            try {
                join(TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
            } catch (InterruptedException e) {
                // Ok. Skjer ikke. Og hvis det skjer, håndteres det under
            }
            if (isAlive()) {
                logger.error("Klarte ikke å stoppe tråden i løpet av {} millisekunder", TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
            }
        }

        public void run() {
            for (;;) {
                for (Steg steg : maskinelleSteg) {
                    if (skalStoppe) {
                        return;
                    }
                    steg.finnBehandlingOgUtfoerSteg();
                    try {
                        sleep(oppholdMellomSteg);
                    } catch (InterruptedException e) {
                        // OK
                    }
                }
            }
        }
    }

}
