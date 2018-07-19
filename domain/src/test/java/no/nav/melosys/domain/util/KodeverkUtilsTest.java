package no.nav.melosys.domain.util;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KodeverkUtilsTest {

    @Test
    public void erGyldigKode() {
        boolean b = KodeverkUtils.erGyldigKode(FagsakType.class, FagsakType.EU_EØS.getKode());
        assertThat(b).isTrue();
    }

    @Test
    public void erNullKode() {
        boolean b = KodeverkUtils.erGyldigKode(FagsakType.class, null);
        assertThat(b).isFalse();
    }

    @Test
    public void erUgyldigKode() {
        boolean b = KodeverkUtils.erGyldigKode(FagsakType.class, FagsakType.EU_EØS.toString());
        assertThat(b).isFalse();
    }

    @Test
    public void hentKodeverk() throws IkkeFunnetException {
        BehandlingType behandlingType = KodeverkUtils.hentKodeverk(BehandlingType.class, BehandlingType.SØKNAD.getKode());
        assertThat(behandlingType).isEqualTo(BehandlingType.SØKNAD);
    }

    @Test(expected = IkkeFunnetException.class)
    public void hentKodeverk_ikkeFunnet() throws IkkeFunnetException {
        KodeverkUtils.hentKodeverk(BehandlingType.class, "ZØKNAD");
    }
}