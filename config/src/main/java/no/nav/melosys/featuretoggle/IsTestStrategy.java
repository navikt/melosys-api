package no.nav.melosys.featuretoggle;

import java.util.Map;

import no.finn.unleash.strategy.Strategy;

class IsTestStrategy implements Strategy {

    private static final String DEV_ENVIRONMENT = "dev";

    private final String currentEnvironment;

    IsTestStrategy(String currentEnvironment) {
        this.currentEnvironment = currentEnvironment;
    }

    @Override
    public String getName() {
        return "isTest";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return DEV_ENVIRONMENT.equalsIgnoreCase(currentEnvironment);
    }
}
