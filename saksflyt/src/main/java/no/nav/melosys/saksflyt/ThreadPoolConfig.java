package no.nav.melosys.saksflyt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "saksflytThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor() {
        // Prioritetskø (se PrioritertSaksflytTaskExecutor / PrioritertProsessinstansOppgave): HØY/NORMAL kjøres
        // foran LAV (batch), FIFO innen samme prioritet. corePoolSize=3 + ubegrenset kø ⇒ alltid 3 arbeidertråder.
        PrioritertSaksflytTaskExecutor executor = new PrioritertSaksflytTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.afterPropertiesSet();
        return executor;
    }
}
