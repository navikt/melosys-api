package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KontrollFactoryTest {

    private final KontrollFactory kontrollFactory = new KontrollFactory();

    @Test
    public void hentKontrollerA003_verifiserKontroller() {
        List<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A003);
        assertThat(kontroller).hasSize(10);
    }

    @Test
    public void hentKontrollerA009_verifiserKontroller() {
        List<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A009);
        assertThat(kontroller).hasSize(10);
    }

    @Test
    public void hentKontrollerA010_verifiserKontroller() {
        List<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A010);
        assertThat(kontroller).hasSize(9);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hentKontrollerA008_verifiserIngenKontroller() {
        List<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A008);
        assertThat(kontroller).isEmpty();
    }
}