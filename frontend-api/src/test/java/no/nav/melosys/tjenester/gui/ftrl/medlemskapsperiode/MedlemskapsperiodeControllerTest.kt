package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.BestemmelseDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(MedlemskapsperiodeController::class)
internal class MedlemskapsperiodeControllerTest {

    @MockkBean
    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val BASE_URL = "/api"
    private val behandlingID: Long = 1231

    @Test
    fun hentMedlemskapsperioder_validerSchema() {
        justRun { aksesskontroll.autoriser(behandlingID) }
        every { medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID) } returns listOf(lagMedlemskapsperiode())

        val expectedJson = """[{
            "id":1231,
            "bestemmelse": "FTRL_KAP2_2_5_FØRSTE_LEDD_E",
            "fomDato": "2024-12-03",
            "tomDato": "2025-12-03",
            "innvilgelsesResultat": "DELVIS_INNVILGET",
            "trygdedekning": "FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON",
            "medlemskapstype": "FRIVILLIG"
        }
        ]""".trimIndent()

        mockMvc.perform(
            get("$BASE_URL/behandlinger/{behandlingID}/medlemskapsperioder", behandlingID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun opprettForslagPåMedlemskapsperioder() {
        justRun { aksesskontroll.autoriserSkriv(behandlingID) }
        every {
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
                behandlingID,
                FTRL_KAP2_2_5_FØRSTE_LEDD_E
            )
        } returns setOf(lagMedlemskapsperiode())

        val req = BestemmelseDto(FTRL_KAP2_2_5_FØRSTE_LEDD_E.name)


        mockMvc.perform(
            post(BASE_URL + "/behandlinger/{behandlingID}/medlemskapsperioder/forslag", behandlingID)
                .content(objectMapper.writeValueAsString(req))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
    }

    @Test
    fun tilbakestillMedlemskapsperioder_tilbakestillNyligOpprettedeMedlemskapsperioder() {
        justRun { aksesskontroll.autoriserSkriv(behandlingID) }
        justRun { medlemskapsperiodeService.tilbakestillNyligOpprettedeMedlemskapsperioder(behandlingID) }

        mockMvc.perform(
            delete("$BASE_URL/behandlinger/{behandlingID}/medlemskapsperioder/tilbakestill", behandlingID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent())

        verify { medlemskapsperiodeService.tilbakestillNyligOpprettedeMedlemskapsperioder(behandlingID) }
    }

    private fun lagMedlemskapsperiode() = Medlemskapsperiode().apply {
        id = behandlingID
        fom = LocalDate.of(2024, 12, 3)
        tom = LocalDate.of(2025, 12, 3)
        innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        bestemmelse = FTRL_KAP2_2_5_FØRSTE_LEDD_E
    }
}
