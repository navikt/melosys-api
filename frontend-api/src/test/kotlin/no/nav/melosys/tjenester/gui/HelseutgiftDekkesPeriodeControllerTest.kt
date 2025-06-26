package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.repository.HelseutgiftDekkesPeriodeRepository
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeController
import no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeDto
import no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [HelseutgiftDekkesPeriodeController::class])
internal class HelseutgiftDekkesPeriodeControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    @MockkBean
    private lateinit var helseutgiftDekkesPeriodeRepository: HelseutgiftDekkesPeriodeRepository

    @BeforeEach
    fun setUp() {
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun hentHelseutgiftDekkesPeriode() {
        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode()
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(any()) } returns helseutgiftDekkesPeriode

        mockMvc.perform(
            get(BASE_URL, 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpectResponseBody(lagHelseutgiftDekkesPeriodeDto(helseutgiftDekkesPeriode))
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode() {
        val helseutgiftDekkesPeriodeDto = lagHelseutgiftDekkesPeriodeDto(lagHelseutgiftDekkesPeriode())
        every { aksesskontroll.autoriserSkriv(any()) } returns Unit
        every {
            helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(
                any(),
                helseutgiftDekkesPeriodeDto.fomDato,
                helseutgiftDekkesPeriodeDto.tomDato,
                Land_iso2.valueOf(helseutgiftDekkesPeriodeDto.bostedLandkode)
            )
        } returns Unit

        mockMvc.perform(
            post(BASE_URL, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(helseutgiftDekkesPeriodeDto))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode_kasterFeil() {
        every { aksesskontroll.autoriserSkriv(any()) } returns Unit

        mockMvc.perform(
            post(BASE_URL, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lagHelseutgiftDekkesPeriodeDtoMedFeilBostedlandkode()))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Landkode er ikke gyldig")));
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode_kasterFeil() {
        every { aksesskontroll.autoriserSkriv(any()) } returns Unit

        mockMvc.perform(
            put(BASE_URL, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lagHelseutgiftDekkesPeriodeDtoMedFeilBostedlandkode()))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Landkode er ikke gyldig")));
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode() {
        val eksisterende = lagHelseutgiftDekkesPeriode()
        val ny = lagHelseutgiftDekkesPeriode(bostedsland = Land_iso2.LT)

        val helseutgiftDekkesPeriodeDto = lagHelseutgiftDekkesPeriodeDto(ny)

        every { aksesskontroll.autoriserSkriv(any()) } returns Unit
        every {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(
                any(),
                helseutgiftDekkesPeriodeDto.fomDato,
                helseutgiftDekkesPeriodeDto.tomDato,
                Land_iso2.valueOf(helseutgiftDekkesPeriodeDto.bostedLandkode)
            )
        } returns ny

        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(any()) } returns eksisterende

        mockMvc.perform(
            put(BASE_URL, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(helseutgiftDekkesPeriodeDto))
        )
            .andExpect(status().isNoContent)
            .andExpectResponseBody(helseutgiftDekkesPeriodeDto)
    }


    fun lagHelseutgiftDekkesPeriode(bostedsland: Land_iso2 = Land_iso2.NO): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = Behandlingsresultat(),
            fomDato = LocalDate.of(2025, 1, 1),
            tomDato = LocalDate.of(2025, 1, 2),
            bostedLandkode = bostedsland
        )
    }

    fun lagHelseutgiftDekkesPeriodeDto(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode): HelseutgiftDekkesPeriodeDto {
        return HelseutgiftDekkesPeriodeDto.av(helseutgiftDekkesPeriode)
    }

    fun lagHelseutgiftDekkesPeriodeDtoMedFeilBostedlandkode(): HelseutgiftDekkesPeriodeDto {
        return HelseutgiftDekkesPeriodeDto(
            fomDato = LocalDate.of(2025, 1, 1),
            tomDato = LocalDate.of(2025, 1, 2),
            bostedLandkode = "Feil"
        )
    }

    companion object {
        private const val BASE_URL = "/api/behandlinger/{behandlingID}/helseutgift-dekkes-perioder"
    }

    private inline fun <reified T> ResultActions.andExpectResponseBody(expectedObject: T): ResultActions =
        this.apply {
            responseBody(objectMapper).containsObjectAsJson(expectedObject, T::class.java)
        }
}
