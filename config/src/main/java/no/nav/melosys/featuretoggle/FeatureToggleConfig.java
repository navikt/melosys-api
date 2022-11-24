package no.nav.melosys.featuretoggle;

import java.util.Collections;
import java.util.List;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.util.UnleashConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FeatureToggleConfig {

    @Bean
    public Unleash unleash(Environment environment) {

        if (!Collections.disjoint(List.of(environment.getActiveProfiles()), List.of("local", "local-mock", "test"))) {
            var fakeUnleash = new FakeUnleash();
            fakeUnleash.enable("melosys.behandle_alle_saker", "melosys.tom_periode_og_land", "melosys.ny_opprett_sak");
            return fakeUnleash;
        } else {
            var unleashConfig = UnleashConfig.builder()
                .appName("melosys")
                .unleashAPI("https://unleash.nais.io/api/")
                .build();

            return new DefaultUnleash(
                unleashConfig,
                new IsTestStrategy(environment.getProperty("APP_ENVIRONMENT")),
                new ByUserIdStrategy()
            );
        }
    }
}
