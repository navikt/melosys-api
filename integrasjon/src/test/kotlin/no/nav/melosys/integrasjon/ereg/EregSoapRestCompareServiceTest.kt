package no.nav.melosys.integrasjon.ereg

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test

class EregSoapRestCompareServiceTest {

    @Test
    fun `fnr skal filtreres bort`() {
        val eregService = mockk<EregService>()
        val eregRestService = mockk<EregRestService>()

        val service = EregSoapRestCompareService(
            FakeUnleash(),
            eregService, eregRestService
        )

        service.filterOrgnummerSomErFnr("12345678901").shouldBe("***********")
    }
}
