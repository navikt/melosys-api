package no.nav.melosys.featuretoggle;

import java.util.Collections;
import java.util.List;

import io.getunleash.DefaultUnleash;
import io.getunleash.FakeUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.GradualRolloutRandomStrategy;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FeatureToggleConfig {

    private final String UNLEASH_URL = "https://melosys-unleash-api.nav.cloud.nais.io/api";

    @Bean
    public Unleash unleash(Environment environment, @Value("${unleash.token}") String token, @Value("${unleash.appname}") String appName) {

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
                .apiKey(token)
                .appName(appName)
                .unleashAPI(UNLEASH_URL)
                .build();

            return new DefaultUnleash(
                unleashConfig,
                new GradualRolloutRandomStrategy(),
                new ByUserIdStrategy()
            );
        }
    }
}
