package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpprettMottatteOpplysningerTest {

    private val mottatteOpplysningerService: MottatteOpplysningerService = mockk(relaxed = true)

    private lateinit var opprettMottatteOpplysninger: OpprettMottatteOpplysninger

    @BeforeEach
    fun setUp() {
        opprettMottatteOpplysninger = OpprettMottatteOpplysninger(mottatteOpplysningerService)
    }

    @Test
    fun `utfør skal kalle opprettSøknadEllerAnmodningEllerAttest`() {
        val prosessinstans = Prosessinstans.forTest { }


        opprettMottatteOpplysninger.utfør(prosessinstans)


        verify { mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(prosessinstans) }
    }
}
