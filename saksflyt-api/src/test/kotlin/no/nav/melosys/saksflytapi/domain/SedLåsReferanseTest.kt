package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test

internal class SedLåsReferanseTest {

    @Test
    fun initierSedLåsReferanse_gyldigReferanseString_verifiserParsing() {
        SedLåsReferanse(RINASAKS_NUMMER + "_" + SED_ID + "_" + VERSJON).apply {
            rinaSaksnummer shouldBe RINASAKS_NUMMER
            sedID shouldBe SED_ID
            sedVersjon shouldBe VERSJON
        }
    }

    @Test
    fun initierSedLåsReferanse_ugyldigReferanseString_kasterException() {
        AssertionsForClassTypes.assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { SedLåsReferanse(RINASAKS_NUMMER + SED_ID + VERSJON) }
    }

    companion object {
        private const val RINASAKS_NUMMER = "123456"
        private const val SED_ID = "P2000"
        private const val VERSJON = "1"
    }
}
