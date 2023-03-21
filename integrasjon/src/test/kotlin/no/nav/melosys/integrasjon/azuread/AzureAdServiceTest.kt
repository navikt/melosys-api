package no.nav.melosys.integrasjon.azuread

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AzureAdServiceTest {

    private var mockRestConsumer = mockk<AzureAdConsumer>()
    private val azureAdService: AzureAdService = AzureAdService(mockRestConsumer)

    @Test
    fun skalHenteSaksbehandlerNavn() {
        val ident = "Z123456"
        val expectedSaksbehandlerNavn = "Lokal Testbruker"
        every {
            mockRestConsumer.hentSaksbehandlerNavn(
                ident
            )
        } returns expectedSaksbehandlerNavn

        val saksbehandlerNavn = azureAdService.hentSaksbehandlerNavn(ident)

        saksbehandlerNavn.shouldBeInstanceOf<String>().shouldBe(expectedSaksbehandlerNavn)
    }
}
