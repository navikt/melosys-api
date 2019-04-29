package no.nav.melosys.tjenester.gui.util;

import io.github.benas.randombeans.api.Randomizer;
import org.apache.commons.lang3.RandomStringUtils;

public class NumericStringRandomizer implements Randomizer<String> {
    private final int lenght;

    public NumericStringRandomizer(int length) {
        this.lenght = length;
    }

    @Override
    public String getRandomValue() {
        return RandomStringUtils.randomNumeric(lenght);
    }
}
