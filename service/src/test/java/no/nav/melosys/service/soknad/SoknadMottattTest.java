package no.nav.melosys.service.soknad;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoknadMottattTest {

    @Test
    void mindreEnn7dagerSidenMottak_erIkkeGammel() {
        SoknadMottatt soknadMottatt = new SoknadMottatt("ID", ZonedDateTime.now().minusDays(6).minusHours(23));

        assertThat(soknadMottatt.erForGammelTilForvaltningsmelding()).isFalse();
    }

    @Test
    void merEnn7dagerSidenMottak_erGammel() {
        SoknadMottatt soknadMottatt = new SoknadMottatt("ID", ZonedDateTime.now().minusDays(7).minusHours(1));

        assertThat(soknadMottatt.erForGammelTilForvaltningsmelding()).isTrue();
    }
}
