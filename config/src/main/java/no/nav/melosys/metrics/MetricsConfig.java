package no.nav.melosys.metrics;

import javax.annotation.PostConstruct;

import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
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
