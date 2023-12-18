package no.nav.melosys.saksflytapi.domain

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test

internal class SedLåsReferanseTest {
    private val rinaSaksnummer = "123"
    private val sedID = "itoghreio"
    private val versjon = "1"

    @Test
    fun initierSedLåsReferanse_gyldigReferanseString_verifiserParsing() {
        AssertionsForClassTypes.assertThat(SedLåsReferanse(rinaSaksnummer + "_" + sedID + "_" + versjon))
            .extracting(
                SedLåsReferanse::referanse,
                SedLåsReferanse::identifikator
            )
            .containsExactly(rinaSaksnummer, sedID + "_" + versjon)
    }

    @Test
    fun initierSedLåsReferanse_ugyldigReferanseString_kasterException() {
        AssertionsForClassTypes.assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { SedLåsReferanse(rinaSaksnummer + sedID + versjon) }
    }
}
