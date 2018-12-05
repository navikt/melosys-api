package no.nav.melosys.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LandkoderTest {
    
    private static final int MELOSYS_LANDKODER = 34;

    @Test
    public void test() {
        int length = Landkoder.values().length;
        assertThat(length).isEqualTo(MELOSYS_LANDKODER);
    }

}