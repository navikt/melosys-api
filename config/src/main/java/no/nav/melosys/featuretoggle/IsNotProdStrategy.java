package no.nav.melosys.featuretoggle;

import java.util.Map;

import no.finn.unleash.strategy.Strategy;

class IsNotProdStrategy implements Strategy {

    private static final String NAMESPACE_Q2 = "q2";

    private final String currentNamespace;

    IsNotProdStrategy(String currentNamespace) {
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