package no.nav.melosys.tjenester.gui.brev

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.service.brev.BrevbestillingFasade
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [BrevbestillingController::class])
class BrevbestillingControllerTest {

    @MockkBean
    private lateinit var brevbestillingFasade: BrevbestillingFasade

    @MockkBean
    private lateinit var brevmalListeBygger: BrevmalListeBygger

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `hentStandardvedleggUtkast skal returnere inline PDF med beskrivende filnavn`() {
        every { brevbestillingFasade.produserStandardvedleggPdf(any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/pdf/utkast/standardvedlegg/{type}", StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", containsString("inline")))
            .andExpect(header().string("Content-Disposition", containsString("standardvedlegg_Viktig informasjon om rettigheter")))
    }

    companion object {
        private const val BASE_URL = "/api/dokumenter/v2"
    }
}
