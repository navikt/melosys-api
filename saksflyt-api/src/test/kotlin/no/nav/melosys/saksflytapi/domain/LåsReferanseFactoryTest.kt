package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class LåsReferanseFactoryTest {

    @Test
    fun `lag låseReferanse for SED`() {
        val sedLåsReferanseString = "1335016_c207c1d6761f467e8bdaadd0eb7e1ed0_4"
        val låsReferanse = LåsReferanseFactory.låsReferanseFraString(sedLåsReferanseString)

        låsReferanse.shouldBeInstanceOf<SedLåsReferanse>()
    }

    @Test
    fun `lag låseReferanse for OpprettManglendeInnbetalingBehandling`() {
        val sedLåsReferanseString = "OMIB_to_be_decided"
        val låsReferanse = LåsReferanseFactory.låsReferanseFraString(sedLåsReferanseString)

        låsReferanse.shouldBeInstanceOf<OpprettManglendeInnbetalingBehandlingLåsReferanse>()
    }
}
