package no.nav.melosys.featuretoggle;

import java.util.Map;

import no.finn.unleash.strategy.Strategy;

class IsTestStrategy implements Strategy {

    private static final String NAMESPACE_Q2 = "q2";

    private final String currentNamespace;

    IsTestStrategy(String currentNamespace) {
        this.currentNamespace = currentNamespace;
    }

    @Override
    public String getName() {
        return "isTest";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return NAMESPACE_Q2.equalsIgnoreCase(currentNamespace);
    }
}