package no.nav.melosys.featuretoggle;

import io.getunleash.*;
import io.getunleash.lang.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;


public final class LocalUnleash implements Unleash {
    private boolean enableAll = false;
    private boolean disableAll = false;
    private Map<String, Boolean> features = new HashMap<>();
    private Map<String, Variant> variants = new HashMap<>();
    private Map<String, Boolean> excludedFeatures = new HashMap<>();

    @Override
    public boolean isEnabled(String toggleName) {
        return isEnabled(toggleName, false);
    }

    @Override
    public boolean isEnabled(String toggleName, boolean defaultSetting) {
        if (enableAll) {
            return excludedFeatures.getOrDefault(toggleName, true);
        } else if (disableAll) {
            return excludedFeatures.getOrDefault(toggleName, false);
        } else {
            return features.getOrDefault(toggleName, defaultSetting);
        }
    }

    @Override
    public boolean isEnabled(String toggleName, UnleashContext context) {
        return Unleash.super.isEnabled(toggleName, context);
    }

    @Override
    public boolean isEnabled(String toggleName, UnleashContext context, boolean defaultSetting) {
        return Unleash.super.isEnabled(toggleName, context, defaultSetting);
    }

    @Override
    public boolean isEnabled(String toggleName, BiPredicate<String, UnleashContext> fallbackAction) {
        return Unleash.super.isEnabled(toggleName, fallbackAction);
    }

    @Override
    public boolean isEnabled(String s, UnleashContext unleashContext, BiPredicate<String, UnleashContext> biPredicate) {
        return false; //TODO se om vi faktisk bruker dette
    }

    @Override
    public Variant getVariant(String toggleName, UnleashContext context) {
        return getVariant(toggleName, Variant.DISABLED_VARIANT);
    }

    @Override
    public Variant getVariant(String toggleName, UnleashContext context, Variant defaultValue) {
        return getVariant(toggleName, defaultValue);
    }

    @Override
    public Variant getVariant(String toggleName) {
        return getVariant(toggleName, Variant.DISABLED_VARIANT);
    }

    @Override
    public Variant getVariant(String toggleName, Variant defaultValue) {
        if (isEnabled(toggleName) && variants.containsKey(toggleName)) {
            return variants.get(toggleName);
        } else {
            return defaultValue;
        }
    }

    @Override
    public List<String> getFeatureToggleNames() {
        return more().getFeatureToggleNames();
    }

    @Override
    public void shutdown() {
        Unleash.super.shutdown();
    }

    @Override
    public MoreOperations more() {
        return new LocalUnleash.FakeMore();
    }

    public void enableAllExcept(String... excludedFeatures) {
        enableAll();
        for (String toggle : excludedFeatures) {
            this.excludedFeatures.put(toggle, false);
        }
    }

    public void disableAllExcept(String... excludedFeatures) {
        disableAll();
        for (String toggle : excludedFeatures) {
            this.excludedFeatures.put(toggle, true);
        }
    }

    public void enableAll() {
        disableAll = false;
        enableAll = true;
        features.clear();
        excludedFeatures.clear();
    }

    public void disableAll() {
        disableAll = true;
        enableAll = false;
        features.clear();
        excludedFeatures.clear();
    }

    public void resetAll() {
        disableAll = false;
        enableAll = false;
        features.clear();
        variants.clear();
        excludedFeatures.clear();
    }

    public void enable(String... features) {
        for (String name : features) {
            this.features.put(name, true);
        }
    }

    public void disable(String... features) {
        for (String name : features) {
            this.features.put(name, false);
        }
    }

    public void reset(String... features) {
        for (String name : features) {
            this.features.remove(name);
        }
    }

    public void setVariant(String t1, Variant a) {
        variants.put(t1, a);
    }

    public class FakeMore implements MoreOperations {

        @Override
        public List<String> getFeatureToggleNames() {
            return new ArrayList<>(features.keySet());
        }

        @Override
        public Optional<FeatureToggle> getFeatureToggleDefinition(String s) {
            return Optional.empty();
        }

        @Override
        public List<EvaluatedToggle> evaluateAllToggles() {
            return evaluateAllToggles(null);
        }

        @Override
        public List<EvaluatedToggle> evaluateAllToggles(@Nullable UnleashContext context) {
            return getFeatureToggleNames().stream()
                .map(
                    toggleName -> {
                        return new EvaluatedToggle(
                            toggleName, isEnabled(toggleName), getVariant(toggleName));
                    })
                .collect(Collectors.toList());
        }

        @Override
        public void count(String toggleName, boolean enabled) {
            // Nothing to count
        }

        @Override
        public void countVariant(String toggleName, String variantName) {
            // Nothing to count
        }
    }
}
