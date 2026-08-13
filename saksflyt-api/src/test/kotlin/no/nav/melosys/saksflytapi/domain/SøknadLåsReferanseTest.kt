package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SøknadLåsReferanseTest {

    @Test
    fun `initier SøknadLåsReferanse med gyldig referanse skal parse korrekt`() {
        SøknadLåsReferanse(GYLDIG_REFERANSE).apply {
            toString() shouldBe GYLDIG_REFERANSE
            // gruppePrefiks = gruppeId-delen (inkl. skilletegn) — felles for relaterte deler.
            gruppePrefiks shouldBe "$GRUPPE_ID" + "_"
        }
    }

    @Test
    fun `gruppePrefiks er lik for to deler i samme gruppe men ulik skjemaId`() {
        val del1 = SøknadLåsReferanse("$GRUPPE_ID" + "_" + "550e8400-e29b-41d4-a716-446655440001")
        val del2 = SøknadLåsReferanse("$GRUPPE_ID" + "_" + "550e8400-e29b-41d4-a716-446655440002")

        del1.gruppePrefiks shouldBe del2.gruppePrefiks
    }

    @Test
    fun `initier SøknadLåsReferanse med ugyldig referanse skal kaste exception`() {
        shouldThrow<IllegalArgumentException> {
            SøknadLåsReferanse(UGYLDIG_REFERANSE)
        }.message shouldBe "$UGYLDIG_REFERANSE er ikke gyldig SØKNAD-referanse ({gruppeId}_{skjemaId})"
    }

    @Test
    fun `bar UUID uten skjemaId-del er ikke lenger gyldig`() {
        shouldThrow<IllegalArgumentException> {
            SøknadLåsReferanse(GRUPPE_ID)
        }
    }

    @Test
    fun `skalSettesPåVent returnerer true når det finnes aktive låsReferanser`() {
        val låsReferanse = SøknadLåsReferanse(GYLDIG_REFERANSE)

        låsReferanse.skalSettesPåVent(listOf("annen-referanse")) shouldBe true
    }

    @Test
    fun `skalSettesPåVent returnerer false når det ikke finnes aktive låsReferanser`() {
        val låsReferanse = SøknadLåsReferanse(GYLDIG_REFERANSE)

        låsReferanse.skalSettesPåVent(emptyList()) shouldBe false
    }

    companion object {
        private const val GRUPPE_ID = "550e8400-e29b-41d4-a716-446655440000"
        private const val SKJEMA_ID = "660e8400-e29b-41d4-a716-446655440000"
        private const val GYLDIG_REFERANSE = GRUPPE_ID + "_" + SKJEMA_ID
        private const val UGYLDIG_REFERANSE = "ikke-en-uuid"
    }
}
