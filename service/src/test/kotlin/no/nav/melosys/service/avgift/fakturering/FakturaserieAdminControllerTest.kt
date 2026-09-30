package no.nav.melosys.service.avgift.fakturering

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [FakturaserieAdminController::class], properties = ["Melosys-admin.apikey=Dummy"])
class FakturaserieAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var controller: FakturaserieAdminController

    @MockkBean
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @Test
    fun `skal returnere saksnummer for fakturaserieReferanse`() {
        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(REFERANSE) } returns listOf(
            Behandlingsresultat.forTest {
                fakturaserieReferanse = REFERANSE
                behandling { fagsak { saksnummer = "MEL-1" } }
            }
        )

        mockMvc.perform(get("/admin/fakturaserier/$REFERANSE/saksnummer"))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"saksnummer":"MEL-1"}"""))
    }

    @Test
    fun `skal kaste IkkeFunnetException når ingen sak har fakturaserieReferanse`() {
        every { behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(REFERANSE) } returns emptyList()

        shouldThrow<IkkeFunnetException> { controller.hentSaksnummer(REFERANSE) }
            .message shouldContain REFERANSE
    }

    companion object {
        private const val REFERANSE = "01HXYZABCDEFGHJKMNPQRSTVWX"
    }
}
