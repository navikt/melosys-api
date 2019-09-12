package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
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

    @Test
    public void erGyldigKode() {
        assertThat(KodeverkUtils.erGyldigKode(Behandlingstyper.class, "SOEKNAD")).isTrue();
    }

    @Test
    public void erGyldigKode_nei() {
        assertThat(KodeverkUtils.erGyldigKode(Behandlingstyper.class, "SSØKNAD")).isFalse();
    }

    @Test
    public void hentAlleKoder() {
        String[] strings = KodeverkUtils.hentAlleKoder(Oppgavetyper.class);
        List<String> list = Arrays.asList(strings);
        assertThat(list).contains("JFR");
        assertThat(list).size().isEqualTo(4);
    }
}