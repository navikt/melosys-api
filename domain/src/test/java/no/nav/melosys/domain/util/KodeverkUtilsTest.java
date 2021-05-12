package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class KodeverkUtilsTest {

    @Test
    void dekod() {
        Behandlingstyper behandlingstype = KodeverkUtils.dekod(Behandlingstyper.class, Behandlingstyper.SOEKNAD.getKode());
        assertThat(behandlingstype).isEqualTo(Behandlingstyper.SOEKNAD);
    }

    @Test
    void dekod_ikkeFunnet() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> KodeverkUtils.dekod(Behandlingstyper.class, "ZØKNAD"))
            .withMessageContaining("finnes ikke");
    }
}
