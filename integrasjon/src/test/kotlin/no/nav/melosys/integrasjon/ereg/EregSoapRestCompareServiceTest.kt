package no.nav.melosys.integrasjon.ereg

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test

class EregSoapRestCompareServiceTest {

    @Test
    fun `fnr skal filtreres bort`() {
        val eregService = mockk<EregService>()
        val eregRestService = mockk<EregRestService>()

        val compareService = EregSoapRestCompareService(
            FakeUnleash(), eregService, eregRestService
        )

        val fnr = "01234567890"
        compareService.finnOrganisasjon(fnr)
            .shouldNotBePresent()

        shouldThrow<TekniskException> {
            compareService.hentOrganisasjon(fnr)
        }.message.shouldContain("orgnr er ikke gyldig")

        shouldThrow<TekniskException> {
            compareService.hentOrganisasjonNavn(fnr)
        }.message.shouldContain("orgnr er ikke gyldig")
    }
}
