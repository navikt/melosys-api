package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadLåsReferanseTest {

    @Test
    fun `initier SøknadLåsReferanse med gyldig UUID skal parse korrekt`() {
        SøknadLåsReferanse(GYLDIG_UUID).apply {
            toString() shouldBe GYLDIG_UUID
            gruppePrefiks shouldBe GYLDIG_UUID
            skjemaId shouldBe UUID.fromString(GYLDIG_UUID)
        }
    }

    @Test
    fun `initier SøknadLåsReferanse med gruppe og skjemaId skal bruke gruppe som gruppePrefiks`() {
        SøknadLåsReferanse("${GRUPPE_UUID}_$GYLDIG_UUID").apply {
            gruppePrefiks shouldBe GRUPPE_UUID
            skjemaId shouldBe UUID.fromString(GYLDIG_UUID)
        }
    }

    @Test
    fun `lag bygger referanse på formatet gruppe_skjemaId`() {
        SøknadLåsReferanse.lag(UUID.fromString(GRUPPE_UUID), UUID.fromString(GYLDIG_UUID))
            .toString() shouldBe "${GRUPPE_UUID}_$GYLDIG_UUID"
    }

    @Test
    fun `gammelt og nytt format i samme gruppe har samme gruppePrefiks`() {
        LåsReferanseFactory.harSammeGruppePrefiks(GRUPPE_UUID, "${GRUPPE_UUID}_$GYLDIG_UUID") shouldBe true
    }

    @Test
    fun `ulike grupper har ulikt gruppePrefiks`() {
        LåsReferanseFactory.harSammeGruppePrefiks("${GRUPPE_UUID}_$GYLDIG_UUID", GYLDIG_UUID) shouldBe false
    }

    @Test
    fun `initier SøknadLåsReferanse med ugyldig referanse skal kaste exception`() {
        shouldThrow<IllegalArgumentException> {
            SøknadLåsReferanse(UGYLDIG_REFERANSE)
        }.message shouldBe "$UGYLDIG_REFERANSE er ikke gyldig SØKNAD-referanse (UUID eller UUID_UUID)"
    }

    @Test
    fun `initier SøknadLåsReferanse med mer enn to UUID-er skal kaste exception`() {
        shouldThrow<IllegalArgumentException> {
            SøknadLåsReferanse("${GRUPPE_UUID}_${GYLDIG_UUID}_$GYLDIG_UUID")
        }
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
        private const val GRUPPE_UUID = "123e4567-e89b-12d3-a456-426614174000"
        private const val UGYLDIG_REFERANSE = "ikke-en-uuid"
    }
}
