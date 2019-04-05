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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;


/**
 * En arbeidertråd som schedulerer arbeid som utføres av de maskinelle stegene.
 * Klassen er ikke en bønne håndtert av Spring, og får allse sine bønne-avhengigheter i konstruktøren.
 *
 * Konfigurasjon:
 * melosys.saksflyt.arbeider.oppholdMellomSteg – Hvor mange millisekunder trådene skal sove mellom hvert steg som aktiveres (default 47)
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class ArbeiderTraad implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ArbeiderTraad.class);

    private long oppholdMellomSteg;

    private Binge binge;

    private ProsessinstansRepository prosessinstansRepo;

    private List<StegBehandler> stegBehandlere;

    private volatile StegBehandler aktivStegBehandler; // Brukes kun for logging

    private volatile Prosessinstans aktivProsessinstans;

    @Autowired
    ArbeiderTraad(Binge binge,
        ProsessinstansRepository prosessinstansRepo,
        List<StegBehandler> stegBehandlere,
        @Value("${melosys.saksflyt.arbeider.oppholdMellomSteg:47}") long oppholdMellomSteg) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
        this.stegBehandlere = stegBehandlere;
        this.oppholdMellomSteg = oppholdMellomSteg;
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            for (StegBehandler stegBehandler : stegBehandlere) {
                try {
                    finnProsessinstansOgUtførSteg(stegBehandler);
                    Thread.sleep(oppholdMellomSteg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Prosessinstanser som avbrytes må registreres som feilet.
                    settTilFeilet();
                } catch (RuntimeException e) {
                    logger.error("Ubehandlet exception! Aktiv stegBehandler: {}.", ClassUtils.getUserClass(aktivStegBehandler.getClass()).getSimpleName(), e);
                    logger.error("Prosessinstans som må ryddes opp i: {}.", aktivProsessinstans.getId());
                    settTilFeilet();
                }
            }
        }
    }

    private void settTilFeilet() {
        try {
            aktivProsessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            prosessinstansRepo.save(aktivProsessinstans);
        } catch (RuntimeException e) {
            logger.error("Prosessinstans {} kunne ikke settes til feilet: ", aktivProsessinstans.getId(), e);
        }
    }

    private void finnProsessinstansOgUtførSteg(StegBehandler stegBehandler) {
        Prosessinstans pi = binge.fjernFørsteProsessinstans(stegBehandler.inngangsvilkår());
        if (pi == null) {
            return;
        }

        aktivStegBehandler = stegBehandler;
        aktivProsessinstans = pi;
        ProsessSteg gammeltSteg = pi.getSteg();
        stegBehandler.utførSteg(pi);

        if (pi.getSteg() != gammeltSteg) {
            pi.setAntallRetry(0);
            pi.setSistForsøkt(LocalDateTime.now());
        }
        pi.setEndretDato(LocalDateTime.now());
        prosessinstansRepo.save(pi); // Kan resultere i DataAccessException

        if (pi.getSteg() != ProsessSteg.FERDIG && pi.getSteg() != ProsessSteg.FEILET_MASKINELT) {
            binge.leggTil(pi);
        }
    }
}
