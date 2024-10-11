package no.nav.melosys.tjenester.gui.aarsavregning

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(controllers = [ÅrsavregningListController::class])
internal class ÅrsavregningListControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var årsavregningService: ÅrsavregningService

    @MockBean
    private lateinit var aksesskontroll: Aksesskontroll


    @Test
    fun `hent liste av årsavregning filtrert på år og type`() {
        every {
            årsavregningService.finnÅrsavregningerPåFagsak(
                "1",
                2023,
                Behandlingsresultattyper.FERDIGBEHANDLET
            )
        } returns listOf(Årsavregning().apply {
            id = 35
            aar = 2023
            behandlingsresultat = Behandlingsresultat().apply {
                type = Behandlingsresultattyper.FERDIGBEHANDLET
                behandling = Behandling().apply {
                    id = 45
                }
            }
        })

        val expectedJson = """[{
            "aarsavregningId": 35,
            "behandlingID": 45,
            "aar": 2023,
            "resultattype": {
              "kode": "FERDIGBEHANDLET",
              "term": "Ferdigbehandlet"
            }
          }]
          """


        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{saksnummer}/aarsavregninger", 1).contentType(
                MediaType.APPLICATION_JSON
            )
                .queryParam("aar", "2023")
                .queryParam("resultattype", "FERDIGBEHANDLET")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(MockMvcResultMatchers.content().json(expectedJson, true))


    }

    companion object {
        private const val BASE_URL: String = "/api/fagsaker"
    }
}
