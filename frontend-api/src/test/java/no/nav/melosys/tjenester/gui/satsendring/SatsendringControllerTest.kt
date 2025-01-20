package no.nav.melosys.tjenester.gui.satsendring

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.AvgiftSatsendringInfo
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [SatsendringController::class])
class SatsendringControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var satsendringFinner: SatsendringFinner

    @Test
    fun `hent satsendringer for spesifikt år`() {
        every { satsendringFinner.finnBehandlingerMedSatsendring(ÅR) } returns lagAvgiftSatsendringInfo()

        val expectedJson = """{
            "år":2025,
            "behandlingerMedSatsendring":{
              "behandlinger":[
                {
                  "behandlingID":1,
                  "saksnummer":"MEL-1",
                  "behandlingstype":{
                    "kode":"SATSENDRING",
                    "term":"Satsendring"
                  },
                  "harSatsendring":true
                }
              ],
              "total":1
            },
            "behandlingerMedSatsendringOgNyVurdering":{
              "behandlinger":[
                {
                  "behandlingID":22,
                  "saksnummer":"MEL-22",
                  "behandlingstype":{
                    "kode":"SATSENDRING",
                    "term":"Satsendring"
                  },
                  "harSatsendring":true
                }
              ],
              "total":1
            },
            "behandlingerUtenSatsendring":{
              "behandlinger":[
                {
                  "behandlingID":333,
                  "saksnummer":"MEL-333",
                  "behandlingstype":{
                    "kode":"SATSENDRING",
                    "term":"Satsendring"
                  },
                  "harSatsendring":false
                }
              ],
              "total":1
            }
        }""".trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/$ÅR/rapport")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    fun lagAvgiftSatsendringInfo() =
        AvgiftSatsendringInfo(
            2025,
            behandlingerMedSatsendring = listOf(
                BehandlingForSatstendring(
                    1, "MEL-1", Behandlingstyper.SATSENDRING, true
                )
            ),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                BehandlingForSatstendring(
                    22, "MEL-22", Behandlingstyper.SATSENDRING, true
                )
            ),
            behandlingerUtenSatsendring = listOf(
                BehandlingForSatstendring(
                    333, "MEL-333", Behandlingstyper.SATSENDRING, false
                ),
            )
        )

    companion object {
        private const val BASE_URL = "/api/satsendringer"
        private const val ÅR = 2025
    }

}
