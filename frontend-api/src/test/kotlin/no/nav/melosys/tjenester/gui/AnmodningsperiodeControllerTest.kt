package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto
import org.hamcrest.Matchers.equalTo
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.stream.Collectors

@WebMvcTest(controllers = [AnmodningsperiodeController::class])
class AnmodningsperiodeControllerTest {

    @MockkBean
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val random = EasyRandom(
        EasyRandomParameters()
            .excludeField(ofType(Behandlingsresultat::class.java))
            .randomize(ofType(LovvalgBestemmelse::class.java)) {
                EnumRandomizer(Lovvalgbestemmelser_883_2004::class.java).randomValue
            }
    )

    @Test
    fun `skal hente anmodningsperioder for behandling`() {
        every { anmodningsperiodeService.hentAnmodningsperioder(1L) } returns mockAnmodningsperioder()
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.anmodningsperioder.length()", equalTo(3)))
    }

    @Test
    fun `skal lagre anmodningsperioder`() {
        val mockAnmodninger = random.objects(Anmodningsperiode::class.java, 3).collect(Collectors.toSet())
        every { anmodningsperiodeService.lagreAnmodningsperioder(any(), any()) } returns mockAnmodninger
        every { aksesskontroll.autoriserSkriv(any()) } returns Unit
        val postDto = AnmodningsperiodePostDto.av(mockAnmodninger)


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.anmodningsperioder.length()", equalTo(3)))


        verify { aksesskontroll.autoriserSkriv(any()) }
        verify { anmodningsperiodeService.lagreAnmodningsperioder(any(), any()) }
    }

    @Test
    fun `skal hente anmodningsperiode svar`() {
        val anmodningsperiode = Anmodningsperiode().apply {
            behandlingsresultat = Behandlingsresultat().apply { id = 1L }
            anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                begrunnelseFritekst = "test"
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            }
        }
        every { anmodningsperiodeService.finnAnmodningsperiode(1L) } returns Optional.of(anmodningsperiode)
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{anmodningsperiodeID}/svar", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.begrunnelseFritekst", equalTo("test")))
            .andExpect(jsonPath("$.anmodningsperiodeSvarType", equalTo(Anmodningsperiodesvartyper.INNVILGELSE.name)))
    }

    @Test
    fun `skal lagre anmodningsperiode svar`() {
        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            begrunnelseFritekst = "fritekst"
            this.anmodningsperiode = anmodningsperiode
        }
        val anmodningsperiode = Anmodningsperiode().apply {
            behandlingsresultat = Behandlingsresultat().apply { id = 1L }
            anmodningsperiodeSvar = svar
        }

        every { anmodningsperiodeService.finnAnmodningsperiode(any()) } returns Optional.of(anmodningsperiode)
        every { anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(any(), any()) } returns svar
        every { aksesskontroll.autoriserSkriv(any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{anmodningsperiodeID}/svar", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AnmodningsperiodeSvarDto.tom()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.begrunnelseFritekst", equalTo("fritekst")))
            .andExpect(jsonPath("$.anmodningsperiodeSvarType", equalTo(Anmodningsperiodesvartyper.INNVILGELSE.name)))


        verify { aksesskontroll.autoriserSkriv(any()) }
    }

    private fun mockAnmodningsperioder(): Set<Anmodningsperiode> =
        random.objects(Anmodningsperiode::class.java, 3).collect(Collectors.toSet())

    companion object {
        private const val BASE_URL = "/api/anmodningsperioder"
    }
}
