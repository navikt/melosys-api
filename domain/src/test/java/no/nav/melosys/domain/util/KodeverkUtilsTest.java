package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.oppgave.Oppgavetype;
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

    @Test
    public void erGyldigKode() {
        assertThat(KodeverkUtils.erGyldigKode(BehandlingType.class, "SKND")).isTrue();
    }

    @Test
    public void erGyldigKode_nei() {
        assertThat(KodeverkUtils.erGyldigKode(BehandlingType.class, "SSØKNAD")).isFalse();
    }

    @Test
    public void hentAlleKoder() {
        String[] strings = KodeverkUtils.hentAlleKoder(Oppgavetype.class);
        List<String> list = Arrays.asList(strings);
        assertThat(list).contains("JFR");
        assertThat(list).size().isEqualTo(2);
    }
}