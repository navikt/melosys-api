package no.nav.melosys.saksflyt.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


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
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class Arbeider {

    private static final int ANNTALL_RETRY_FOR_Å_STOPPE_TRÅDER = 40;
    private static final int OPPHOLD_MELLOM_RETRY_FOR_Å_STOPPE_TRÅDER = 10000;

    private static final Logger logger = LoggerFactory.getLogger(Arbeider.class);

    private int antallTråder;

    // Liste med arbeidstråder. Disse er prototype bønner med tilstand og tråd.
    private ArbeiderTraad[] tråder;

    @Autowired
    public Arbeider(
        Binge binge, 
        ProsessinstansRepository prosessinstansRepo, 
        List<StegBehandler> stegBehandlere,
        @Value("${melosys.saksflyt.arbeider.oppholdMellomSteg:47}") long oppholdMellomSteg,
        @Value("${melosys.saksflyt.arbeider.antallTråder:1}") int antallTråder
    ) {
        this.antallTråder = antallTråder;
        tråder = new ArbeiderTraad[antallTråder];
        for (int i = 0; i < antallTråder; i++) {
            tråder[i] = new ArbeiderTraad(binge, prosessinstansRepo, stegBehandlere, oppholdMellomSteg);
        }
    }
    
    /**
     * Starter prosessering. Skal kun kalles av spring etter at alt er injisert.
     */
    @PostConstruct
    public void start() {
        for (int i = 0; i < antallTråder; i++) {
            tråder[i].start();
        }
        logger.info("Startet {} arbeidertråder", antallTråder);
    }

    /**
     * Stopper prosessering. Skal kun kalles av spring når konteksten skal tas ned.
     */
    @PreDestroy
    public void stopp() {
        // ADVARSEL: IKKE rør denne metoden med mindre du vet nøyaktig hva du gjør (og har god kompetanse på hvordan tråder fungerer i Java) 
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
        logger.error("MULIG BEHOV FOR MANUELL OPPRYDDING: Søk etter loggmelding om hvilken StegBehandler som ikke lot seg stoppe " +
            "og sjekk konsistensen for relaterte prosessinstanser i databasen.");
    }

}
