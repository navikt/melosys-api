package no.nav.melosys.featuretoggle;

import io.getunleash.strategy.Strategy;

import java.util.Map;


class IsTestStrategy implements Strategy {

    private static final String DEV_ENVIRONMENT = "dev";
    private static final String Q1_ENVIRONMENT = "q1";

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
        return DEV_ENVIRONMENT.equalsIgnoreCase(currentEnvironment) || Q1_ENVIRONMENT.equalsIgnoreCase(currentEnvironment);
    }
}
