package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;


/**
 * En arbeidertråd som schedulerer arbeid som utføres av de maskinelle stegene.
 * Klassen er ikke en bønne håndtert av Spring, og får allse sine bønne-avhengigheter i konstruktøren.
 */
public class ArbeiderTraad extends Thread {

    private static final long TIMEOUT_FOR_Å_STOPPE_EN_TRÅD = 60000;

    private static final Logger logger = LoggerFactory.getLogger(ArbeiderTraad.class);

    private long oppholdMellomSteg;

    private Binge binge;

    private ProsessinstansRepository prosessinstansRepo;

    private List<StegBehandler> stegBehandlere;

    private volatile StegBehandler aktivStegBehandler; // Brukes kun for logging

    private volatile long aktivProsessinstans; // Brukes kun for logging

    @SuppressWarnings("unused")
    private ArbeiderTraad() {} // Skal ikke håndteres av Spring
    
    public ArbeiderTraad(Binge binge, ProsessinstansRepository prosessinstansRepo, List<StegBehandler> stegBehandlere, long oppholdMellomSteg) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
        this.stegBehandlere = stegBehandlere;
        this.oppholdMellomSteg = oppholdMellomSteg;
    }

    // Kalles av Arbeider når konteksten tas ned
    void stoppArbeider() {
        // ADVARSEL: IKKE rør denne metoden med mindre du vet nøyaktig hva du gjør (og har god kompetanse på hvordan tråder fungerer i Java) 
        if (!isAlive()) {
            return;
        }
        interrupt(); // Merk: ikke kjørende tråd sin interrupt som kalles
        try {
            join(TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
        } catch (InterruptedException e) {
            // Vi er her bare hvis tråden som forsøker å ta ned arbeiderne blir interrupted.
            // Disse interruptene kan skape problemer hvis det blir for mange av dem, siden vi avbryter venting på at en tråd stopper.
            logger.error("Forsøk på å vente på en tråds død ble avbrutt.");
            Thread.currentThread().interrupt(); // Resetter interrupted-statusen til kallende tråd
        }
        if (isAlive()) {
            logger.error("Klarte ikke å stoppe tråden i løpet av {} millisekunder", TIMEOUT_FOR_Å_STOPPE_EN_TRÅD);
            logger.error("StegBehandler som ikke lot seg stoppe: {}", aktivStegBehandler.getClass().getName());
            logger.error("Prosessinstans som kanskje må ryddes opp i: {}", aktivProsessinstans);
        }
    }

    @Override
    public void run() {
        // ADVARSEL: IKKE rør denne metoden med mindre du vet nøyaktig hva du gjør (og har god kompetanse på hvordan tråder fungerer i Java) 
        for (;;) {
            for (StegBehandler stegBehandler : stegBehandlere) {
                if (interrupted()) {
                    return;
                }
                try {
                    finnProsessinstansOgUtførSteg(stegBehandler);
                    try {
                        sleep(oppholdMellomSteg);
                    } catch (InterruptedException e) {
                        return;
                    }
                } catch (Exception e) {
                    // Vi er her hvis en StegBehandler kastet en Exception 
                    logger.error("Stanser prosessering pga. ubehandlet Exception", e);
                    logger.error("Aktiv stegBehandler: {}", aktivStegBehandler.getClass().getName());
                    logger.error("Prosessinstans som kanskje må ryddes opp i: {}", aktivProsessinstans);
                    return;
                }
            }
        }
    }
    
    private void finnProsessinstansOgUtførSteg(StegBehandler stegBehandler) throws DataAccessException {
        Prosessinstans pi = binge.fjernFørsteProsessinstans(stegBehandler.inngangsvilkår());
        if (pi == null) {
            return;
        }

        aktivStegBehandler = stegBehandler;
        aktivProsessinstans = pi.getId();
        ProsessSteg gammeltSteg = pi.getSteg();
        stegBehandler.utførSteg(pi);

        if (pi.getSteg() != gammeltSteg) {
            pi.setAntallRetry(0);
            pi.setSistForsøkt(LocalDateTime.now());
        }
        pi.setEndretDato(LocalDateTime.now());
        prosessinstansRepo.save(pi); // Kan resultere i DataAccessException, som kastes videre og merfører at tråden stoppes.

        if (pi.getSteg() != null) {
            binge.leggTil(pi);
        }
    }

}
