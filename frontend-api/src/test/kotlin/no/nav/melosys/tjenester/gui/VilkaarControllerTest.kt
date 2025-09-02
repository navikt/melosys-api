package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Ressurs
import no.nav.melosys.service.vilkaar.InngangsvilkaarService
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [VilkaarController::class])
class VilkaarControllerTest {

    @MockkBean
    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    @MockkBean
    private lateinit var inngangsvilkaarService: InngangsvilkaarService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal hente vilkår for behandling`() {
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { vilkaarsresultatService.hentVilkaar(1L) } returns lagVilkaarDTOList()


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.[0].vilkaar", equalTo(Vilkaar.FO_883_2004_ART12_1.kode)))
    }

    @Test
    fun `skal registrere vilkår for behandling`() {
        val dto = lagVilkaarDTOList()
        every { aksesskontroll.autoriserSkrivTilRessurs(1L, Ressurs.VILKÅR) } returns Unit
        every { vilkaarsresultatService.registrerVilkår(1L, any<List<VilkaarDto>>()) } returns Unit
        every { vilkaarsresultatService.hentVilkaar(1L) } returns lagVilkaarDTOList()


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.[0].vilkaar", equalTo(Vilkaar.FO_883_2004_ART12_1.kode)))
    }

    @Test
    fun `skal overstyre inngangsvilkår til oppfylt`() {
        every { aksesskontroll.autoriserSkrivTilRessurs(1L, Ressurs.VILKÅR) } returns Unit
        every { inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L) } returns Unit


        mockMvc.perform(
            put("$BASE_URL/{behandlingID}/inngangsvilkaar/overstyr", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)
    }

    private fun lagVilkaarDTOList() = listOf(
        VilkaarDto().apply {
            vilkaar = Vilkaar.FO_883_2004_ART12_1.kode
            begrunnelseKoder = setOf(Utsendt_arbeidstaker_begrunnelser.ERSTATTER_ANNEN.kode)
        }
    )

    companion object {
        private const val BASE_URL = "/api/vilkaar"
    }
}
