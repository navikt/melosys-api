package no.nav.melosys.saksflyt.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;


/**
 * Komponent med arbeidertråder som schedulerer arbeid som utføres av de maskinelle stegene.
 * 
 * Dette er en passe dum implementasjon, der x tråder hver for seg looper gjennom alle agenter og aktiverer dem. Dette gjentas i det uendelige.
 * 
 * Konfigurasjon:
 *     melosys.saksflyt.arbeider.antallTråder – Antall tråder (default 1)
 *
 */
@Component
@Scope(SCOPE_SINGLETON)
public class Saksflyt {

    private static final Logger logger = LoggerFactory.getLogger(Saksflyt.class);

    private int antallTråder;

    private final ThreadPoolTaskExecutor taskExecutor;

    // Liste med arbeidstråder. Disse er prototype bønner med tilstand og tråd.
    private final ArbeiderTraad[] tråder;

    private final List<Future<?>> arbeidere;

    @Autowired
    public Saksflyt(
        ApplicationContext context,
        @Qualifier("applicationTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
        @Value("${melosys.saksflyt.arbeider.antallTråder:1}") int antallTråder
    ) {
        this.taskExecutor = taskExecutor;
        this.antallTråder = antallTråder;
        tråder = new ArbeiderTraad[antallTråder];
        arbeidere = new ArrayList<>();
        for (int i = 0; i < antallTråder; i++) {
            tråder[i] = context.getBean(ArbeiderTraad.class);
        }
    }
    
    /**
     * Starter prosessering.
     */
    @EventListener
    public void start(ApplicationReadyEvent event) {
        for (int i = 0; i < antallTråder; i++) {
            arbeidere.add(taskExecutor.submit(tråder[i]));
        }
        logger.info("Startet {} arbeidertråder", antallTråder);
    }

    public boolean saksflytLever() {
        return !arbeidere.isEmpty() && arbeidere.stream().noneMatch(future -> future.isDone() || future.isCancelled());
    }
}
