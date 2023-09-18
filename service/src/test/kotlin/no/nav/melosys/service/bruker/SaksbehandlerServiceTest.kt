package no.nav.melosys.service.bruker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.integrasjon.azuread.AzureAdService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaksbehandlerServiceTest {
    private val LOKAL_IDENT = "Z123456"
    private val LOKAL_SAKSBEHANDLER_NAVN = "Lokal Testbruker"

    private var azureAdService = mockk<AzureAdService>()
    private val subjectHandler: SubjectHandler = mockk<SpringSubjectHandler>()
    private lateinit var saksbehandlerService: SaksbehandlerService

    @BeforeEach
    fun setup() {
        saksbehandlerService = SaksbehandlerService(azureAdService)
        SubjectHandler.set(subjectHandler)
    }

    @Test
    fun `hent saksbehandler navn fra AD`() {
        every { azureAdService.hentSaksbehandlerNavn(LOKAL_IDENT) } returns LOKAL_SAKSBEHANDLER_NAVN

        val saksbehandlerNavn = saksbehandlerService.finnNavnForIdent(LOKAL_IDENT).get()

        verify { azureAdService.hentSaksbehandlerNavn(LOKAL_IDENT) }

        saksbehandlerNavn.shouldBe(LOKAL_SAKSBEHANDLER_NAVN)
    }
}
