package no.nav.melosys.tjenester.gui.saksflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto
import no.nav.melosys.tjenester.gui.dto.IkkeGodkjennUnntaksperiodeDto
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [UnntaksperiodeController::class])
class UnntaksperiodeControllerTest {

    @MockkBean
    private lateinit var unntaksperiodeService: UnntaksperiodeService
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal godkjenne unntaksperiode`() {
        val periodeDto = PeriodeDto(LocalDate.of(2001, 1, 1), LocalDate.of(2002, 1, 1))
        val dto = GodkjennUnntaksperiodeDto(
            true,
            "tekst",
            periodeDto,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1.toString()
        )
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every { unntaksperiodeService.godkjennPeriode(1L, any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/godkjenn", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `skal ikke godkjenne unntaksperiode`() {
        val dto = IkkeGodkjennUnntaksperiodeDto(emptySet(), "fritekst")
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every { unntaksperiodeService.ikkeGodkjennPeriode(1L, any(), any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/ikkegodkjenn", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isNoContent)
    }

    companion object {
        private const val BASE_URL = "/api/saksflyt/unntaksperioder"
    }
}