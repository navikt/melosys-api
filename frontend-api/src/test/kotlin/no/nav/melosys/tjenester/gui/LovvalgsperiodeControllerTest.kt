package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.lovvalgsperiode.OpprettLovvalgsperiodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [LovvalgsperiodeController::class])
internal class LovvalgsperiodeControllerTest {

    @MockkBean
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var opprettLovvalgsperiodeService: OpprettLovvalgsperiodeService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal hente eksisterende lovvalgsperiode og gi 200 OK og en forekomst`() {
        every { aksesskontroll.autoriser(13L) } returns Unit
        every { lovvalgsperiodeService.hentLovvalgsperioder(13L) } returns lagLovvalgsperiode()


        testHentLovvalgsperioder(13L, listOf(FORVENTET))


        verify { aksesskontroll.autoriser(13L) }
    }

    @Test
    fun `skal hente ikke-eksisterende lovvalgsperiode og gi 200 OK og tom JSON`() {
        every { aksesskontroll.autoriser(Long.MAX_VALUE) } returns Unit
        every { lovvalgsperiodeService.hentLovvalgsperioder(Long.MAX_VALUE) } returns emptyList()


        testHentLovvalgsperioder(Long.MAX_VALUE, emptyList())


        verify { aksesskontroll.autoriser(Long.MAX_VALUE) }
    }

    @Test
    fun `skal hente lovvalgsperiode uten tilgang`() {
        every { aksesskontroll.autoriser(10L) } throws SikkerhetsbegrensningException("Computer says no")


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError())
            .andExpect(responseBody(objectMapper).containsError("message", "Computer says no"))


        verify { aksesskontroll.autoriser(10L) }
    }

    @Test
    fun `skal hente lovvalgsperiode med teknisk feil`() {
        every { aksesskontroll.autoriser(15L) } throws TekniskException("Det har oppstått en...")


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", 15L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is5xxServerError())
            .andExpect(responseBody(objectMapper).containsError("message", "Det har oppstått en..."))


        verify { aksesskontroll.autoriser(15L) }
    }

    @Test
    fun `hentOpprinneligLovvalgsperiode returnerer periode`() {
        val fomDato = LocalDate.of(2018, 12, 12)
        val tomDato = LocalDate.of(2019, 12, 12)
        val lovvalgsperiode = Lovvalgsperiode().apply {
            setFom(fomDato)
            setTom(tomDato)
        }

        every { aksesskontroll.autoriser(5L) } returns Unit
        every { lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(5L) } returns lovvalgsperiode


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}/opprinnelig", 5L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opprinneligLovvalgsperiode.fom", equalTo("2018-12-12")))
            .andExpect(jsonPath("$.opprinneligLovvalgsperiode.tom", equalTo("2019-12-12")))


        verify { aksesskontroll.autoriser(5L) }
    }

    @Test
    fun `skal lagre en lovvalgsperiode og gi 200 OK og ekko`() {
        val lovvalgsperiodeDtos = listOf(FORVENTET)

        every { aksesskontroll.autoriserSkriv(42L) } returns Unit
        every { lovvalgsperiodeService.lagreLovvalgsperioder(42L, any()) } returns emptyList()


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lovvalgsperiodeDtos))
        )
            .andExpect(status().isOk())
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    lovvalgsperiodeDtos,
                    object : TypeReference<List<LovvalgsperiodeDto>>() {}
                )
            )


        verify {
            lovvalgsperiodeService.lagreLovvalgsperioder(42L, any())
            aksesskontroll.autoriserSkriv(42L)
        }
    }

    private fun testHentLovvalgsperioder(behandlingsid: Long, forventet: Collection<LovvalgsperiodeDto>) {
        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", behandlingsid)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize<Any>(forventet.size)))
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    forventet,
                    object : TypeReference<List<LovvalgsperiodeDto>>() {}
                )
            )
    }

    private fun lagLovvalgsperiode() = listOf(
        Lovvalgsperiode().apply {
            id = FORVENTET.periodeID.toLong()
            fom = FORVENTET.periode.fom
            tom = FORVENTET.periode.tom
            dekning = Trygdedekninger.FULL_DEKNING_EOSFO
            lovvalgsland = Land_iso2.valueOf(FORVENTET.lovvalgsland)
            bestemmelse = Lovvalgbestemmelser_883_2004.valueOf(FORVENTET.lovvalgsbestemmelse)
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.valueOf(FORVENTET.tilleggBestemmelse)
            innvilgelsesresultat = InnvilgelsesResultat.valueOf(FORVENTET.innvilgelsesResultat)
            medlemskapstype = Medlemskapstyper.valueOf(FORVENTET.medlemskapstype)
            medlPeriodeID = FORVENTET.medlemskapsperiodeID.toLong()
        }
    )

    companion object {
        private val FOM = LocalDate.now()
        private val FORVENTET = LovvalgsperiodeDto(
            "1",
            PeriodeDto(FOM, FOM),
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
            Land_iso2.SK,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FULL_DEKNING_EOSFO,
            Medlemskapstyper.FRIVILLIG,
            "10"
        )

        private const val BASE_URL = "/api/lovvalgsperioder"
    }
}
