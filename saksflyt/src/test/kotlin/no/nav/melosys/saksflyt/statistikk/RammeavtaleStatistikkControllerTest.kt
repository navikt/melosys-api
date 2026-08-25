package no.nav.melosys.saksflyt.statistikk

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [RammeavtaleStatistikkController::class], properties = ["Melosys-admin.apikey=Dummy"])
class RammeavtaleStatistikkControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var rammeavtaleStatistikkService: RammeavtaleStatistikkService

    @Test
    fun `uten parametre hentes hele perioden med saksnummer`() {
        every { rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null, true) } returns statistikk()

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{
                        "antall": 2,
                        "fom": null,
                        "tom": null,
                        "antallPerVedtaksaar": {"2025": 2},
                        "saker": [
                            {"saksnummer": "MEL-1", "vedtaksaar": "2025", "vedtaksdato": "2025-03-04"},
                            {"saksnummer": "MEL-2", "vedtaksaar": "2025", "vedtaksdato": "2025-06-01"}
                        ]
                    }""",
                ),
            )

        verify(exactly = 1) { rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null, true) }
    }

    @Test
    fun `fom og tom bindes som datoer og sendes videre`() {
        every {
            rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                true,
            )
        } returns statistikk(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 12, 31))

        mockMvc.perform(get(BASE_URL).param("fom", "2025-01-01").param("tom", "2025-12-31"))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"fom": "2025-01-01", "tom": "2025-12-31"}"""))
    }

    @Test
    fun `inkluderSaksnummer false gir saker som eksplisitt null i responsen`() {
        every {
            rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null, false)
        } returns statistikk(saker = null)

        mockMvc.perform(get(BASE_URL).param("inkluderSaksnummer", "false"))
            .andExpect(status().isOk)
            // Feltet skal være til stede med null, ikke utelatt — konsumenter deserialiserer mot samme type
            .andExpect(content().json("""{"antall": 2, "antallPerVedtaksaar": {"2025": 2}, "saker": null}"""))
    }

    private fun statistikk(
        fom: LocalDate? = null,
        tom: LocalDate? = null,
        saker: List<RammeavtaleSak>? = listOf(
            RammeavtaleSak("MEL-1", "2025", LocalDate.of(2025, 3, 4)),
            RammeavtaleSak("MEL-2", "2025", LocalDate.of(2025, 6, 1)),
        ),
    ) = RammeavtaleFjernarbeidStatistikk(
        antall = 2,
        fom = fom,
        tom = tom,
        antallPerVedtaksaar = sortedMapOf("2025" to 2L),
        saker = saker,
    )

    companion object {
        private const val BASE_URL = "/admin/statistikk/rammeavtale-fjernarbeid"
    }
}
