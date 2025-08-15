package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Institusjon
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Collections.emptyList
import java.util.Collections.singletonList

@WebMvcTest(controllers = [EessiController::class])
class EessiControllerKtTest {

    @MockkBean
    private lateinit var eessiService: EessiService

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun hentMottakerinstitusjoner() {
        every { eessiService.hentEessiMottakerinstitusjoner(any(), any()) } returns singletonList(institusjonBelgia)


        mockMvc.perform(
            get("$BASE_URL/mottakerinstitusjoner/{bucType}", BucType.LA_BUC_01)
                .contentType(MediaType.APPLICATION_JSON)
                .param("landkoder", Landkoder.BE.kode)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].landkode", equalTo(Landkoder.BE.kode)))
    }

    @Test
    fun opprettBuc() {
        val dto = BucBestillingDto(BucType.LA_BUC_01, singletonList(Landkoder.BE.kode), emptyList())
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { eessiService.opprettBucOgSed(any(), any(), any(), any()) } returns "url"


        mockMvc.perform(
            post("$BASE_URL/bucer/{behandlingID}/opprett", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun hentBucer() {
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { behandlingService.hentBehandling(any()) } returns Behandling.forTest {
            id = 1L
            fagsak {
                gsakSaksnummer = 987654321L
            }
        }
        every { eessiService.hentTilknyttedeBucer(any(), any()) } returns emptyList()


        mockMvc.perform(
            get("$BASE_URL/bucer/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .param("statuser", "A")
        )
            .andExpect(status().isOk)
    }

    companion object {
        private const val BASE_URL = "/api/eessi"
        private const val mottakerBelgia = "BE:12222"
        private val institusjonBelgia = Institusjon(mottakerBelgia, null, Landkoder.BE.kode)
    }


}
