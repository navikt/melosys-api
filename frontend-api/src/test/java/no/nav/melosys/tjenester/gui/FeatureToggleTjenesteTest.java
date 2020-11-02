package no.nav.melosys.tjenester.gui;

import java.util.Map;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class FeatureToggleTjenesteTest {

    private FeatureToggleTjeneste featureToggleTjeneste;

    @BeforeEach
    public void setup() {
        FakeUnleash fakeUnleash = new FakeUnleash();
        fakeUnleash.enableAll();

        featureToggleTjeneste = new FeatureToggleTjeneste(fakeUnleash);
    }

    @Test
    void hentFeatureToggle_alleEnabled_verifiserAlleErEnablet() {
        String featureEn = "melosys.feature.en";
        String featureTo = "melosys.feature.to";

        Map<String, Boolean> res = featureToggleTjeneste.hentFeatureToggles(Set.of(featureEn, featureTo)).getBody();

        assertThat(res)
            .containsOnly(
                entry(featureEn, Boolean.TRUE),
                entry(featureTo, Boolean.TRUE)
            );
    }

}