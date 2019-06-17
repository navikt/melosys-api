package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Landkoder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LandkoderTest {

    private static final int EØS_MEDLEMER_OG_SVEITS = 32; // EU + EØS + EFTA
    private static final int TERRITORIER = 4; // Grønland, Færøyene, Åland, Svalbard+JanMayen

    @Test
    public void test() {
        int length = Landkoder.values().length;
        assertThat(length).isEqualTo(EØS_MEDLEMER_OG_SVEITS + TERRITORIER);
    }
}