package no.nav.melosys.integrasjon.azuread

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AzureAdServiceTest {

    private var mockRestClient = mockk<AzureAdClient>()
    private val azureAdService: AzureAdService = AzureAdService(mockRestClient)

    @Test
    fun skalHenteSaksbehandlerNavn() {
        val ident = "Z123456"
        val expectedSaksbehandlerNavn = "Lokal Testbruker"
        every {
            mockRestClient.hentSaksbehandlerNavn(
                ident
            )
        } returns expectedSaksbehandlerNavn

        val saksbehandlerNavn = azureAdService.hentSaksbehandlerNavn(ident)

        saksbehandlerNavn.shouldBeInstanceOf<String>().shouldBe(expectedSaksbehandlerNavn)
    }
}
