package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import no.nav.melosys.domain.eessi.SedType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


class UfmKontrollsettTest {

    @Test
    fun hentKontrollerA001_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A001)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA003_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A003)
        Assertions.assertThat(kontroller).hasSize(11)
    }

    @Test
    fun hentKontrollerA009_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A009)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA010_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A010)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA008_verifiserIngenKontroller() {
        Assertions.assertThatExceptionOfType(UnsupportedOperationException::class.java)
            .isThrownBy { UfmKontrollsett.hentRegelsettForSedType(SedType.A008) }
            .withMessageContaining("A008 er ikke støttet")
    }
}
