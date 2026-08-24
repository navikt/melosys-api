package no.nav.melosys.saksflytapi.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LåsReferanseFactoryTest {

    @Test
    fun `lag låseReferanse for SED`() {
        val sedLåsReferanseString = "1335016_c207c1d6761f467e8bdaadd0eb7e1ed0_4"
        val låsReferanse = LåsReferanseFactory.lagLåsReferanse(sedLåsReferanseString)

        låsReferanse.shouldBeInstanceOf<SedLåsReferanse>()
    }

    @Test
    fun `lag låseReferanse for OpprettManglendeInnbetalingBehandling`() {
        val sedLåsReferanseString = LåsReferanseFactory.lagString(
            ManglendeFakturabetalingMelding(
                fakturaserieReferanse = "01HHFM03YMHHQAVZ4SQF9Y29E4",
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = "123456789"
            )
        )
        val låsReferanse = LåsReferanseFactory.lagLåsReferanse(sedLåsReferanseString)

        låsReferanse.shouldBeInstanceOf<ManglendeInnbetalingBehandlingLåsReferanse>()
            .låsReferanse.shouldBe("UBETALT_01HHFM03YMHHQAVZ4SQF9Y29E4_123456789")
    }

    @Test
    fun `lag låseReferanse for SØKNAD`() {
        val søknadLåsReferanseString = "550e8400-e29b-41d4-a716-446655440000_660e8400-e29b-41d4-a716-446655440000"
        val låsReferanse = LåsReferanseFactory.lagLåsReferanse(søknadLåsReferanseString)

        låsReferanse.shouldBeInstanceOf<SøknadLåsReferanse>()
        låsReferanse.gruppePrefiks shouldBe "550e8400-e29b-41d4-a716-446655440000_"
    }
}
