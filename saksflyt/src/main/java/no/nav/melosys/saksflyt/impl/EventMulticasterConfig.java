package no.nav.melosys.saksflyt.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class EventMulticasterConfig { // todo pakke og wrapper
    @Bean("melosysHendelseMulticaster")
    public ApplicationEventMulticaster melosysHendelseMulticaster() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();

        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        // todo eventMulticaster.setErrorHandler();

        return eventMulticaster;
    }
}
