package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.service.sak.ÅrsavregningService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [ÅrsavregningTjeneste::class])
internal class ÅrsavregningTjenesteTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var årsavregningService: ÅrsavregningService

    @Test
    fun `hent avregning basert på ID`() {
        every { årsavregningService.hentÅrsavregnig(any()) } returns Aarsavregning()

        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{avregningID}", 1).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
    }

    companion object {
        private const val BASE_URL: String = "/api/aarsavregninger"
    }
}
