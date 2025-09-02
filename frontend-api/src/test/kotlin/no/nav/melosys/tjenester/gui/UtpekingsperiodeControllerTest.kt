package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.ofType
import org.jeasy.random.randomizers.misc.EnumRandomizer
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

    private val random = EasyRandom(
        EasyRandomParameters()
            .randomizationDepth(2)
            .randomize(ofType(LovvalgBestemmelse::class.java)) {
                EnumRandomizer(Lovvalgbestemmelser_883_2004::class.java).randomValue
            }
            .randomize(SaksopplysningDokument::class.java) { PersonDokument() }
            .randomize(ofType(Behandlingsresultat::class.java)) { null }
    )

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
        Utpekingsperiode(
            LocalDate.now(),
            LocalDate.now(),
            Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
            null
        ),
        random.nextObject(Utpekingsperiode::class.java),
        random.nextObject(Utpekingsperiode::class.java)
    )

    companion object {
        private const val BASE_URL = "/api/utpekingsperioder"
    }
}
