package no.nav.melosys.metrics;

import io.micrometer.spring.autoconfigure.MeterRegistryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {

    @Bean
    MeterRegistryConfigurer configurer() {
        return registry -> registry.config().commonTags("team", "melosys");
    }
}
