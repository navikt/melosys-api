package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import no.nav.melosys.domain.eessi.SedType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


class UfmKontrollsettTest {

    @Test
    fun hentKontrollerA001_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A001, false)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA003_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A003, false)
        Assertions.assertThat(kontroller).hasSize(11)
    }

    @Test
    fun hentKontrollerA009_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A009, false)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA010_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A010, false)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA001_CDM4_3_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A001, true)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA003_CDM4_3_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A003, true)
        Assertions.assertThat(kontroller).hasSize(11)
    }

    @Test
    fun hentKontrollerA009_CDM4_3_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A009, true)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA010_CDM4_3_verifiserKontroller() {
        val kontroller =
            UfmKontrollsett.hentRegelsettForSedType(SedType.A010, true)
        Assertions.assertThat(kontroller).hasSize(10)
    }

    @Test
    fun hentKontrollerA008_verifiserIngenKontroller() {
        Assertions.assertThatExceptionOfType(UnsupportedOperationException::class.java)
            .isThrownBy { UfmKontrollsett.hentRegelsettForSedType(SedType.A008, false) }
            .withMessageContaining("A008 er ikke støttet")
    }
}
