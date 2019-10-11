package no.nav.melosys.domain.util;

import org.junit.Test;

import static no.nav.melosys.domain.dokument.adresse.AdresseUtils.sammenslå;
import static org.assertj.core.api.Assertions.assertThat;

public class AdresseUtilsTest {

    @Test
    public void concatTest() {
        assertThat(sammenslå(null, "145")).isEqualTo("145");
        assertThat(sammenslå("Gate", null)).isEqualTo("Gate");
        assertThat(sammenslå("Gate", "1234")).isEqualTo("Gate 1234");
        assertThat(sammenslå(" Gate 1234 ", null)).isEqualTo("Gate 1234");
    }
}
