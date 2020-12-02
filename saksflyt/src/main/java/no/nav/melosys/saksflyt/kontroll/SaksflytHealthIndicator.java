package no.nav.melosys.saksflyt.kontroll;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class SaksflytHealthIndicator implements HealthIndicator {

    private final ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor;

    public SaksflytHealthIndicator(ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor) {
        this.saksflytThreadPoolTaskExecutor = saksflytThreadPoolTaskExecutor;
    }

    @Override
    public Health health() {
        ThreadPoolExecutor executor = saksflytThreadPoolTaskExecutor.getThreadPoolExecutor();
        return (executor.isShutdown() || executor.isTerminated() || executor.isTerminating())
            ? Health.down().build()
            : Health.up().build();
    }
}
