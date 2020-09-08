package no.nav.melosys.saksflyt.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;


@Component
@Scope(SCOPE_SINGLETON)
public class SaksflytArbeiderPool {
    private static final Logger logger = LoggerFactory.getLogger(SaksflytArbeiderPool.class);

    private final ExecutorService taskExecutor;
    private final SaksflytArbeider[] tråder;
    private final List<Future<?>> futures;

    private static final int ANTALL_TRÅDER = 1;

    @Autowired
    public SaksflytArbeiderPool(
        ApplicationContext context,
        @Qualifier("applicationTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
        MeterRegistry registry
    ) {
        this.taskExecutor = ExecutorServiceMetrics.monitor(registry, taskExecutor.getThreadPoolExecutor(), "saksflyt");
        tråder = new SaksflytArbeider[ANTALL_TRÅDER];
        futures = new ArrayList<>();
        for (int i = 0; i < ANTALL_TRÅDER; i++) {
            tråder[i] = context.getBean(SaksflytArbeider.class);
        }
    }

    @EventListener
    public void start(ApplicationReadyEvent event) {
        for (int i = 0; i < ANTALL_TRÅDER; i++) {
            futures.add(taskExecutor.submit(tråder[i]));
        }
        logger.info("Startet {} arbeidertråder", ANTALL_TRÅDER);
    }

    public boolean saksflytLever() {
        return !futures.isEmpty() && futures.stream().noneMatch(future -> future.isDone() || future.isCancelled());
    }
}
