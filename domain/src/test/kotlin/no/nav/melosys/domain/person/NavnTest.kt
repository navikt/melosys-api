package no.nav.melosys.domain.person;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NavnTest {

    private static final String NAVN_ETTERNAVN_SIST = "Per Olav Mellomnavn Etternavn";
    private static final String NAVN_ETTERNAVN_FØRST = "Etternavn, Per Olav Mellomnavn";

    @Test
    public void navnEtternavnFørst() {
        assertThat(Navn.navnEtternavnFørst(NAVN_ETTERNAVN_SIST)).isEqualTo(NAVN_ETTERNAVN_FØRST);
    }

    @Test
    void navnEtternavnSist() {
        assertThat(Navn.navnEtternavnSist(NAVN_ETTERNAVN_FØRST)).isEqualTo(NAVN_ETTERNAVN_SIST);
    }

    @Test
    void riktigFormatNavnKommerUtRiktig() {
        assertThat(Navn.navnEtternavnSist(NAVN_ETTERNAVN_SIST)).isEqualTo(NAVN_ETTERNAVN_SIST);
    }
}
