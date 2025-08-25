package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.adresse.Adresse.Companion.sammenslå
import org.junit.jupiter.api.Test

internal class AdresseTest {
    @Test
    fun concatTest() {
        sammenslå(null, "145") shouldBe "145"
        sammenslå("Gate", null) shouldBe "Gate"
        sammenslå("Gate", "1234") shouldBe "Gate 1234"
        sammenslå(" Gate 1234 ", null) shouldBe "Gate 1234"
    }
}