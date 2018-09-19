package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KodeverkUtilsTest {

    @Test
    public void dekod() throws IkkeFunnetException {
        Behandlingstype behandlingstype = KodeverkUtils.dekod(Behandlingstype.class, Behandlingstype.SØKNAD.getKode());
        assertThat(behandlingstype).isEqualTo(Behandlingstype.SØKNAD);
    }

    @Test(expected = IkkeFunnetException.class)
    public void dekod_ikkeFunnet() throws IkkeFunnetException {
        KodeverkUtils.dekod(Behandlingstype.class, "ZØKNAD");
    }

    @Test
    public void erGyldigKode() {
        assertThat(KodeverkUtils.erGyldigKode(Behandlingstype.class, "SOEKNAD")).isTrue();
    }

    @Test
    public void erGyldigKode_nei() {
        assertThat(KodeverkUtils.erGyldigKode(Behandlingstype.class, "SSØKNAD")).isFalse();
    }

    @Test
    public void hentAlleKoder() {
        String[] strings = KodeverkUtils.hentAlleKoder(Oppgavetype.class);
        List<String> list = Arrays.asList(strings);
        assertThat(list).contains("JFR");
        assertThat(list).size().isEqualTo(2);
    }
}