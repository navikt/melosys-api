package no.nav.melosys.domain.person;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NavnTest {

    private static final String NAVN_STANDARD = "Per Olav Mellomnavn Etternavn";
    private static final String NAVN_ETTERNAVN_FØRST = "Etternavn, Per Olav Mellomnavn";

    @Test
    public void navnEtternavnFørst() {
        assertThat(Navn.navnEtternavnFørst(NAVN_STANDARD)).isEqualTo(NAVN_ETTERNAVN_FØRST);
    }

    @Test
    void navnEtternavnSist() {
        assertThat(Navn.navnEtternavnSist(NAVN_ETTERNAVN_FØRST)).isEqualTo(NAVN_STANDARD);
    }
}
