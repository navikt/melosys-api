package no.nav.melosys.metrics;

import javax.annotation.PostConstruct;

import io.micrometer.spring.autoconfigure.MeterRegistryCustomizer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableSpringBootMetricsCollector
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<?> registryCustomizer() {
        return registry -> registry.config().commonTags("team", "melosys");
    }

    // Denne gjør at Micrometer og Prometheus sin simpleclient bruker samme registry
    @Bean
    CollectorRegistry prometheusCollector() {
        return CollectorRegistry.defaultRegistry;
    }

    @PostConstruct
    public void prometheusHotspotConfig() {
        DefaultExports.initialize();
    }
}
