package no.nav.melosys.domain.util;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KodeverkUtilsTest {

    @Test
    public void dekod() throws IkkeFunnetException {
        BehandlingType behandlingType = KodeverkUtils.dekod(BehandlingType.class, BehandlingType.SØKNAD.getKode());
        assertThat(behandlingType).isEqualTo(BehandlingType.SØKNAD);
    }

    @Test(expected = IkkeFunnetException.class)
    public void dekod_ikkeFunnet() throws IkkeFunnetException {
        KodeverkUtils.dekod(BehandlingType.class, "ZØKNAD");
    }
}