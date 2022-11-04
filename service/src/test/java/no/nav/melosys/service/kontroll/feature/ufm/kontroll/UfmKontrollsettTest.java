package no.nav.melosys.service.kontroll.feature.ufm.kontroll;

import java.util.Set;
import java.util.function.Function;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UfmKontrollsettTest {

    private FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    public void beforeEach() {
        unleash.enableAll();
    }

    @Test
    void hentKontrollerA001_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A001, unleash);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA003_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A003, unleash);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA009_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A009, unleash);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA010_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A010, unleash);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA008_verifiserIngenKontroller() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> UfmKontrollsett.hentRegelsettForSedType(SedType.A008, unleash))
            .withMessageContaining("A008 er ikke støttet");
    }
}
