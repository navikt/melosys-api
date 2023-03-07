package no.nav.melosys.service.kontroll.feature.ufm.kontroll;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UfmKontrollsettTest {

    @Test
    void hentKontrollerA001_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A001);
        assertThat(kontroller).hasSize(10);
    }

    @Test
    void hentKontrollerA003_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A003);
        assertThat(kontroller).hasSize(11);
    }

    @Test
    void hentKontrollerA009_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A009);
        assertThat(kontroller).hasSize(10);
    }

    @Test
    void hentKontrollerA010_verifiserKontroller() {
        Set<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A010);
        assertThat(kontroller).hasSize(10);
    }

    @Test
    void hentKontrollerA008_verifiserIngenKontroller() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> UfmKontrollsett.hentRegelsettForSedType(SedType.A008))
            .withMessageContaining("A008 er ikke støttet");
    }
}
