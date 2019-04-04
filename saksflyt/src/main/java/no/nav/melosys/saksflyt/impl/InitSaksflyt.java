package no.nav.melosys.saksflyt.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
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
public class InitSaksflyt {

    private static final Logger logger = LoggerFactory.getLogger(InitSaksflyt.class);

    private int antallTråder;

    private final TaskExecutor taskExecutor;

    // Liste med arbeidstråder. Disse er prototype bønner med tilstand og tråd.
    private ArbeiderTraad[] tråder;

    @Autowired
    public InitSaksflyt(
        ApplicationContext context,
        @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
        @Value("${melosys.saksflyt.arbeider.antallTråder:1}") int antallTråder
    ) {
        this.taskExecutor = taskExecutor;
        this.antallTråder = antallTråder;
        tråder = new ArbeiderTraad[antallTråder];
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
            taskExecutor.execute(tråder[i]);
        }
        logger.info("Startet {} arbeidertråder", antallTråder);
    }

}
