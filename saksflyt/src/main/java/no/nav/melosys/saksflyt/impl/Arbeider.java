package no.nav.melosys.saksflyt.impl;

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

import no.nav.melosys.saksflyt.api.Agent;


/**
 * Komponent med arbeidertråder som schedulerer arbeid som utføres av de maskinelle stegene.
 * 
 * Dette er en passe dum implementasjon, der x tråder hver for seg looper gjennom alle agenter og aktiverer dem. Dette gjentas i det uendelige.
 * 
 * Konfigurasjon:
 *     melosys.saksflyt.arbeider.antallTråder – Antall tråder (default 1)
 *     melosys.saksflyt.arbeider.oppholdMellomSteg – Hvor mange millisekunder trådene skal sove mellom hvert steg som aktiveres (default 47)
 *
 */
@Component
@Scope("singleton")
public class Arbeider {

    private static final int ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER = 40;
    private static final int OPPHOLD_MELLOM_RETRY_FOR_Å_STOPPE_TRÅDER = 10000;
    private static final long TIMEOUT_FOR_Å_STOPPE_EN_TRÅD = 60000;

    private static final Logger logger = LoggerFactory.getLogger(Arbeider.class);

    /** Antall arbeidstråder */
    @Value("${melosys.saksflyt.arbeider.antallTråder:1}")
    private int antallTråder;

    /** Opphold mellom hvert steg */
    @Value("${melosys.saksflyt.arbeider.oppholdMellomSteg:47}")
    private long oppholdMellomSteg;

    @Autowired
    private List<Agent> agenter;

    private ArbeiderTraad[] tråder;

    private void loggAgenter() {
        if (!logger.isInfoEnabled()) {
            return;
        }
        List<String> klasser = new ArrayList<>();
        for (Agent agent : agenter) {
            Class<?> c = agent.getClass();
            for (;;) { // Iterer superklasser helt til vi ikke har en proxy
                if (c.getName().startsWith("no.nav") || c.getName().startsWith("java.")) {
                    break;
                }
                c = c.getSuperclass();
            }
            klasser.add(c.getName());
        }
        klasser.sort(String.CASE_INSENSITIVE_ORDER);
        logger.info("Agenter som aktiveres av arbeideren:");
        klasser.stream().forEach(logger::info);
    }

    /**
     * Starter prosessering. Skal kun kalles av spring etter at alt er injisert.
     */
    @PostConstruct
    public void start() {
        loggAgenter();
        tråder = new ArbeiderTraad[antallTråder];
        for (int i = 0; i < antallTråder; i++) {
            tråder[i] = new ArbeiderTraad();
            tråder[i].start();
        }
        logger.info("Startet {} arbeidertråder", antallTråder);
    }

    /**
     * Stopper prosessering. Skal kun kalles av spring når konteksten skal tas ned.
     */
    @PreDestroy
    public void stopp() {
        for (int forsøk = 1; forsøk <= ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER; forsøk++) { // Ytre løkke for retry på å stoppe alle tråder
            logger.info("Forsøk nr. {} av {} på å stoppe alle arbeidertråder...", forsøk, ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER);
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
                Thread.currentThread().interrupt();
            }
        }
        // Vi er her bare hvis vi ikke klarte å stoppe alle trådene
        // Alt vi kan gjøre nå er å logge problemet
        logger.error("KRITISK FEIL: Ga opp å stoppe alle arbeidertråder etter {} forsøk", ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER);
        logger.error("MULIG BEHOV FOR MANUELL OPPRYDDING: Søk etter loggmelding om hvilken agent som ikke lot seg stoppe " +
            "og sjekk konsistensen for relaterte prosessinstanser i databasen.");
    }

    private class ArbeiderTraad extends Thread {
        
        private volatile Agent aktivAgent; // Brukes kun for å logge avt. agent som ikke lot seg stoppe

        public void stoppArbeider() {
            if (!isAlive()) {
                return;
            }
            interrupt(); 
            try {
                join(TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Ellers ignorerer vi forsøk på interrupt, siden vi har et sterkt ønske om å stoppe alle arbeidere.
            }
            if (isAlive()) {
                logger.error("Klarte ikke å stoppe tråden i løpet av {} millisekunder", TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
                logger.error("Agent som ikke lot seg stoppe:" + aktivAgent.getClass().getName());
            }
        }

        @Override
        public void run() {
            for (;;) {
                for (Agent agent : agenter) {
                    if (interrupted()) {
                        return;
                    }
                    aktivAgent = agent;
                    agent.finnProsessinstansOgUtførSteg();
                    try {
                        sleep(oppholdMellomSteg);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

}
