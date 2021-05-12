package no.nav.melosys.service.kontroll.ufm;

import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class KontrollFactoryTest {
    private final KontrollFactory kontrollFactory = new KontrollFactory();

    @Test
    void hentKontrollerA001_verifiserKontroller() {
        List<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A001);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA003_verifiserKontroller() {
        List<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A003);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA009_verifiserKontroller() {
        List<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A009);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA010_verifiserKontroller() {
        List<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller = kontrollFactory.hentKontrollerForSedType(SedType.A010);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA008_verifiserIngenKontroller() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() ->kontrollFactory.hentKontrollerForSedType(SedType.A008))
            .withMessageContaining("A008 er ikke støttet");
    }
}
