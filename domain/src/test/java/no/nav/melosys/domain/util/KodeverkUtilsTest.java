package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KodeverkUtilsTest {

    @Test
    public void dekod() throws IkkeFunnetException {
        Behandlingstyper behandlingstype = KodeverkUtils.dekod(Behandlingstyper.class, Behandlingstyper.SOEKNAD.getKode());
        assertThat(behandlingstype).isEqualTo(Behandlingstyper.SOEKNAD);
    }

    @Test(expected = IkkeFunnetException.class)
    public void dekod_ikkeFunnet() throws IkkeFunnetException {
        KodeverkUtils.dekod(Behandlingstyper.class, "ZØKNAD");
    }
}