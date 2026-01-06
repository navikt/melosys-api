package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.utpekingsperiodeForTest
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [UtpekingsperiodeController::class])
internal class UtpekingsperiodeControllerTest {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var utpekingService: UtpekingService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal hente utpekingsperioder for behandling`() {
        val utpekingsperioder = lagUtpekingsperioder()
        val utpekingsperioderDto = UtpekingsperioderDto.av(utpekingsperioder)

        every { aksesskontroll.autoriser(123L) } returns Unit
        every { utpekingService.hentUtpekingsperioder(123L) } returns utpekingsperioder


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", 123L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(utpekingsperioderDto, UtpekingsperioderDto::class.java))


        verify { aksesskontroll.autoriser(123L) }
    }

    @Test
    fun `skal lagre utpekingsperioder for behandling`() {
        val utpekingsperioder = lagUtpekingsperioder()
        val utpekingsperioderDto = UtpekingsperioderDto.av(utpekingsperioder)

        every { aksesskontroll.autoriserSkriv(123L) } returns Unit
        every { utpekingService.lagreUtpekingsperioder(123L, any()) } returns utpekingsperioder


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", 123L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(utpekingsperioderDto))
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(utpekingsperioderDto, UtpekingsperioderDto::class.java))


        verify { aksesskontroll.autoriserSkriv(123L) }
    }

    private fun lagUtpekingsperioder() = listOf(
        utpekingsperiodeForTest {
            fom = LocalDate.now()
            tom = LocalDate.now()
            lovvalgsland = Land_iso2.SE
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        },
        utpekingsperiodeForTest {
            fom = LocalDate.now().minusMonths(3)
            tom = LocalDate.now().minusMonths(1)
            lovvalgsland = Land_iso2.DK
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1
        },
        utpekingsperiodeForTest {
            fom = LocalDate.now().plusMonths(1)
            tom = LocalDate.now().plusMonths(6)
            lovvalgsland = Land_iso2.FI
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
        }
    )

    companion object {
        private const val BASE_URL = "/api/utpekingsperioder"
    }
}
