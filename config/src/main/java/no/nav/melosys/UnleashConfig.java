package no.nav.melosys;

import java.util.Map;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.strategy.Strategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class UnleashConfig {

    @Bean
    public Unleash unleash(Environment environment) {
        var config = no.finn.unleash.util.UnleashConfig.builder()
            .appName("melosys")
            .unleashAPI("https://unleash.nais.adeo.no/api/")
            .build();

        Strategy isNotProdStrategy = new IsNotProdStrategy(environment.getProperty("NAIS_NAMESPACE"));

        return new DefaultUnleash(config, isNotProdStrategy);
    }

    private static class IsNotProdStrategy implements Strategy {

        private static final String NAMESPACE_Q2 = "q2";

        private final String currentNamespace;

        private IsNotProdStrategy(String currentNamespace) {
            this.currentNamespace = currentNamespace;
        }

        @Override
        public String getName() {
            return "isNotProd";
        }

        @Override
        public boolean isEnabled(Map<String, String> map) {
            return NAMESPACE_Q2.equalsIgnoreCase(currentNamespace);
        }
    }
}
