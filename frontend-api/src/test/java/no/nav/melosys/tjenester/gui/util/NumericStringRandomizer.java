package no.nav.melosys.tjenester.gui.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.api.Randomizer;

public class NumericStringRandomizer implements Randomizer<String> {
    private final int length;

    public NumericStringRandomizer(int length) {
        this.length = length;
    }

    @Override
    public String getRandomValue() {
        return RandomStringUtils.randomNumeric(length);
    }
}
