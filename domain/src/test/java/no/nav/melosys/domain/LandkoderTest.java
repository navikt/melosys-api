package no.nav.melosys.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LandkoderTest {
    
    private static final int ANTALL_EØS_MEDLEMER = 31;

    @Test
    public void test() {
        int length = Landkoder.values().length;
        assertThat(length).isEqualTo(ANTALL_EØS_MEDLEMER);
    }

}