package no.nav.melosys.service.bruker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.integrasjon.azuread.AzureAdService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SaksbehandlerServiceTest {
    private val LOKAL_IDENT = "Z123456"
    private val LOKAL_SAKSBEHANDLER_NAVN = "Lokal Testbruker"

    @MockK
    private lateinit var azureAdService: AzureAdService


    @Test
    fun hentNavnForIdent_finnerSaksbehandlerNavn_ok() {
        val saksbehandlerService = SaksbehandlerService(azureAdService)
        every { azureAdService.hentSaksbehandlerNavn(LOKAL_IDENT) } returns LOKAL_SAKSBEHANDLER_NAVN

        val saksbehandlerNavn = saksbehandlerService.hentNavnForIdent(LOKAL_IDENT)

        verify { azureAdService.hentSaksbehandlerNavn(LOKAL_IDENT) }

        saksbehandlerNavn.shouldBe(LOKAL_SAKSBEHANDLER_NAVN)
    }
}
