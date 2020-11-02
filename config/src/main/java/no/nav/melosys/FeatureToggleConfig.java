package no.nav.melosys;

import java.util.Arrays;
import java.util.Map;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.strategy.Strategy;
import no.finn.unleash.util.UnleashConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FeatureToggleConfig {

    @Bean
    public Unleash unleash(Environment environment) {

        if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
            var fakeUnleash = new FakeUnleash();
            fakeUnleash.enableAll();
            return fakeUnleash;
        } else {
            var unleashConfig = UnleashConfig.builder()
                .appName("melosys")
                .unleashAPI("https://unleash.nais.io/api/")
                .build();

            Strategy isNotProdStrategy = new IsNotProdStrategy(environment.getProperty("NAIS_NAMESPACE"));
            return new DefaultUnleash(unleashConfig, isNotProdStrategy);
        }
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
