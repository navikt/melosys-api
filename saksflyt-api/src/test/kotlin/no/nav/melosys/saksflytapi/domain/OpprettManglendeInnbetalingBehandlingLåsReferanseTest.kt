package no.nav.melosys.saksflytapi.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OpprettManglendeInnbetalingBehandlingLåsReferanseTest {

    @Test
    fun initierLåsReferanse_gyldigReferanseString_verifiserParsing() {
        OpprettManglendeInnbetalingBehandlingLåsReferanse(PREFIX + "_" + FAKTURASERIE_REFERANSE + "_" + FAKTURANUMMER).apply {
            fakturaserieReferanse shouldBe FAKTURASERIE_REFERANSE
            fakturanummer shouldBe FAKTURANUMMER
        }
    }

    @Test
    fun initierLåsReferanse_ugyldigReferanseString_kasterException() {
        shouldThrow<IllegalArgumentException> {
            OpprettManglendeInnbetalingBehandlingLåsReferanse(FAKTURASERIE_REFERANSE)
        }.message.shouldBe("$FAKTURASERIE_REFERANSE er ikke gyldig OpprettManglendeInnbetalingBehandling-referanse")
    }

    companion object {
        private val PREFIX = LåsReferanseType.UBETALT.toString()
        private const val FAKTURASERIE_REFERANSE = "01HHFM03YMHHQAVZ4SQF9Y29E4"
        private const val FAKTURANUMMER = "123456789"
    }
}
