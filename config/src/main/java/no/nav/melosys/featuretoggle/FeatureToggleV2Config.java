package no.nav.melosys.featuretoggle;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.util.UnleashConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;

@Configuration
public class FeatureToggleV2Config {

    @Bean
    public Unleash unleash(Environment environment) {

        if (!Collections.disjoint(List.of(environment.getActiveProfiles()), List.of("local", "local-mock", "local-q2"))) {
            var localUnleash = new LocalUnleash();
            localUnleash.enableAll();
            return localUnleash;
        } else if(List.of(environment.getActiveProfiles()).contains("test")) {
            var fakeUnleash = new FakeUnleash();
            fakeUnleash.enableAll();
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
