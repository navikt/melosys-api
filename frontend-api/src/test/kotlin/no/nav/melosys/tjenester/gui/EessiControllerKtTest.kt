package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Institusjon
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Collections.emptyList
import java.util.Collections.singletonList
import org.mockito.Mockito.`when` as whenMock

@WebMvcTest(controllers = [EessiController::class])
class EessiControllerKtTest {

    @MockBean
    private lateinit var eessiService: EessiService

    @MockBean
    private lateinit var behandlingService: BehandlingService

    @MockBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val BASE_URL = "/api/eessi"
    private val mottakerBelgia = "BE:12222"
    private val institusjonBelgia = Institusjon(mottakerBelgia, null, Landkoder.BE.kode)
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(FagsakTestFactory.builder().medGsakSaksnummer().build())
            .build()
    }

    @Test
    fun hentMottakerinstitusjoner() {
        whenMock(eessiService.hentEessiMottakerinstitusjoner(anyString(), anyCollection())).thenReturn(singletonList(institusjonBelgia))

        mockMvc.perform(
            get("$BASE_URL/mottakerinstitusjoner/{bucType}", BucType.LA_BUC_01)
                .contentType(MediaType.APPLICATION_JSON)
                .param("landkoder", Landkoder.BE.kode)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].landkode").value(Landkoder.BE.kode))
    }

    @Test
    fun opprettBuc() {
        val dto = BucBestillingDto(BucType.LA_BUC_01, singletonList(Landkoder.BE.kode), emptyList())
        whenMock(eessiService.opprettBucOgSed(anyLong(), any(), anyList(), anyCollection())).thenReturn("url")

        mockMvc.perform(
            post("$BASE_URL/bucer/{behandlingID}/opprett", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun hentBucer() {
        whenMock(behandlingService.hentBehandling(anyLong())).thenReturn(behandling)

        mockMvc.perform(
            get("$BASE_URL/bucer/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .param("statuser", "A")
        )
            .andExpect(status().isOk)
    }
}
