package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SøknadLåsReferanseTest {

    @Test
    fun `initier SøknadLåsReferanse med gyldig UUID skal parse korrekt`() {
        SøknadLåsReferanse(GYLDIG_UUID).apply {
            toString() shouldBe GYLDIG_UUID
            gruppePrefiks shouldBe GYLDIG_UUID
        }
    }

    @Test
    fun `initier SøknadLåsReferanse med ugyldig referanse skal kaste exception`() {
        shouldThrow<IllegalArgumentException> {
            SøknadLåsReferanse(UGYLDIG_REFERANSE)
        }.message shouldBe "$UGYLDIG_REFERANSE er ikke gyldig SØKNAD-referanse (UUID)"
    }

    @Test
    fun `skalSettesPåVent returnerer true når det finnes aktive låsReferanser`() {
        val låsReferanse = SøknadLåsReferanse(GYLDIG_UUID)

        låsReferanse.skalSettesPåVent(listOf("annen-referanse")) shouldBe true
    }

    @Test
    fun `skalSettesPåVent returnerer false når det ikke finnes aktive låsReferanser`() {
        val låsReferanse = SøknadLåsReferanse(GYLDIG_UUID)

        låsReferanse.skalSettesPåVent(emptyList()) shouldBe false
    }

    companion object {
        private const val GYLDIG_UUID = "550e8400-e29b-41d4-a716-446655440000"
        private const val UGYLDIG_REFERANSE = "ikke-en-uuid"
    }
}
