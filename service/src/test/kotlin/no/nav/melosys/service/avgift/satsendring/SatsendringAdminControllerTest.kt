package no.nav.melosys.service.avgift.satsendring

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.AvgiftSatsendringInfo
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(controllers = [SatsendringAdminController::class], properties = ["Melosys-admin.apikey=Dummy"])
class SatsendringAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var satsendringFinner: SatsendringFinner

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var prosessinstansService: ProsessinstansService

    @Test
    fun `Hent satsendringer for spesifikt år`() {
        every { satsendringFinner.finnBehandlingerMedSatsendring(ÅR) } returns lagAvgiftSatsendringInfo()

        val expectedJson = """{
            "år":2025,
            "behandlingerMedSatsendring":{
              "behandlinger":[
                {
                  "behandlingID":1,
                  "saksnummer":"MEL-1"
                }
              ],
              "total":1
            },
            "behandlingerMedSatsendringOgNyVurdering":{
              "behandlinger":[
                {
                  "behandlingID":22,
                  "saksnummer":"MEL-22"
                }
              ],
              "total":1
            },
            "behandlingerUtenSatsendring":{
              "behandlinger":[
                {
                  "behandlingID":333,
                  "saksnummer":"MEL-333"
                }
              ],
              "total":1
            },
            "behandlingerSomFeilet":{
              "behandlinger":[
                {
                  "behandlingID":444,
                  "saksnummer":"MEL-444",
                  "feilAarsak":"Feilet mot beregning"
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

    @Test
    fun `Oppretter prosessinstans for satsendringsbehandling`() {
        every { satsendringFinner.finnBehandlingerMedSatsendring(ÅR) } returns lagAvgiftSatsendringInfo()
        val behandling = Behandling().apply { id = 1 }
        every { behandlingService.hentBehandling(1) } returns behandling
        val randomUUID = UUID.randomUUID()
        every { prosessinstansService.opprettSatsendringBehandlingFor(behandling, ÅR) } returns randomUUID


        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/$ÅR/fagsaker/MEL-1")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(content().string("Oppretter satsendring prosessinstans: $randomUUID for sak MEL-1 og behandlingID: 1"))
    }

    private fun lagAvgiftSatsendringInfo() =
        AvgiftSatsendringInfo(
            2025,
            behandlingerMedSatsendring = listOf(
                BehandlingForSatstendring(
                    1, "MEL-1", Behandlingstyper.SATSENDRING, true, false
                )
            ),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                BehandlingForSatstendring(
                    22, "MEL-22", Behandlingstyper.SATSENDRING, true, true
                )
            ),
            behandlingerUtenSatsendring = listOf(
                BehandlingForSatstendring(
                    333, "MEL-333", Behandlingstyper.SATSENDRING, false, false
                ),
            ),
            behandlingerSomFeilet = listOf(
                BehandlingForSatstendring(
                    444, "MEL-444", Behandlingstyper.SATSENDRING, false, false, feilÅrsak = "Feilet mot beregning"
                ),
            ),
        )

    companion object {
        private const val BASE_URL = "/admin/satsendringer"
        private const val ÅR = 2025
    }

}
