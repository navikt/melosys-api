package no.nav.melosys.domain;

import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.dokument.adresse.Adresse.sammenslå;
import static org.assertj.core.api.Assertions.assertThat;

public class AdresseTest {
    @Test
    public void concatTest() {
        assertThat(sammenslå(null, "145")).isEqualTo("145");
        assertThat(sammenslå("Gate", null)).isEqualTo("Gate");
        assertThat(sammenslå("Gate", "1234")).isEqualTo("Gate 1234");
        assertThat(sammenslå(" Gate 1234 ", null)).isEqualTo("Gate 1234");
    }
}
