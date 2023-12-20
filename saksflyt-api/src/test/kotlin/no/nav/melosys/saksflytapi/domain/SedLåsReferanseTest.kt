package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
        shouldThrow<IllegalArgumentException> {
            SedLåsReferanse(RINASAKS_NUMMER + SED_ID + VERSJON)
        }.message.shouldBe("$RINASAKS_NUMMER$SED_ID$VERSJON er ikke gyldig SED-referanse")
    }

    companion object {
        private const val RINASAKS_NUMMER = "123456"
        private const val SED_ID = "P2000"
        private const val VERSJON = "1"
    }
}
