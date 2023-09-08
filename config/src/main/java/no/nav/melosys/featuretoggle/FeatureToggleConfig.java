package no.nav.melosys.featuretoggle;

import java.util.Collections;
import java.util.List;

import io.getunleash.DefaultUnleash;
import io.getunleash.FakeUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.GradualRolloutRandomStrategy;
import io.getunleash.strategy.GradualRolloutSessionIdStrategy;
import io.getunleash.strategy.GradualRolloutUserIdStrategy;
import io.getunleash.util.UnleashConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FeatureToggleConfig {

    private final String UNLEASH_URL = "https://melosys-unleash-api.nav.cloud.nais.io/api";
    private static final Logger log = LoggerFactory.getLogger(FeatureToggleConfig.class);

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
                .apiKey(environment.getProperty("unleash.token"))
                .appName(environment.getProperty("unleash.appname"))
                .projectName("default")
                .unleashAPI(UNLEASH_URL)
                .build();

            log.info("Debug melosys q1 unleash: " + unleashConfig.getAppName() + " " + UNLEASH_URL);

            return new DefaultUnleash(
                unleashConfig,
                new GradualRolloutSessionIdStrategy(),
                new GradualRolloutUserIdStrategy(),
                new GradualRolloutRandomStrategy(),
                new ByUserIdStrategy()
            );
        }
    }
}
