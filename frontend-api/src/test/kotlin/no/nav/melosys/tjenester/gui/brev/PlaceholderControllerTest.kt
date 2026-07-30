package no.nav.melosys.tjenester.gui.brev

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.Unleash
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.placeholder.PlaceholderDefinisjon
import no.nav.melosys.service.placeholder.PlaceholderService
import no.nav.melosys.service.placeholder.PlaceholderVerdi
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PlaceholderController::class])
internal class PlaceholderControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    private lateinit var placeholderService: PlaceholderService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var unleash: Unleash

    @BeforeEach
    fun setUp() {
        SpringSubjectHandler.set(TestSubjectHandler())
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER) } returns true
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER_DYNAMISK_PLACEHOLDER) } returns true
        every { aksesskontroll.autoriser(any()) } returns Unit
    }

    @Test
    fun `katalogen returnerer placeholdere med kontraktens feltnavn`() {
        every { placeholderService.hentKatalog() } returns listOf(
            PlaceholderDefinisjon(
                nokkel = "saksnummer",
                visningsnavn = "Saksnummer",
                beskrivelse = "Sakens saksnummer i Melosys",
                eksempel = { "2024/123456" },
                resolver = { it.behandling.fagsak.saksnummer },
            ),
        )

        mockMvc.perform(get(KATALOG_URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.placeholdere.length()").value(1))
            .andExpect(jsonPath("$.placeholdere[0].nokkel").value("saksnummer"))
            .andExpect(jsonPath("$.placeholdere[0].visningsnavn").value("Saksnummer"))
            .andExpect(jsonPath("$.placeholdere[0].beskrivelse").value("Sakens saksnummer i Melosys"))
            .andExpect(jsonPath("$.placeholdere[0].eksempel").value("2024/123456"))
            .andExpect(jsonPath("$.placeholdere[0].sakstyper.length()").value(0))
    }

    @Test
    fun `verdier returnerer resolvede noekler for behandlingen`() {
        every { placeholderService.hentVerdier(BEH_ID) } returns listOf(
            PlaceholderVerdi(nokkel = "saksnummer", verdi = "2024/123456"),
        )

        mockMvc.perform(get(VERDIER_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.verdier.length()").value(1))
            .andExpect(jsonPath("$.verdier[0].nokkel").value("saksnummer"))
            .andExpect(jsonPath("$.verdier[0].verdi").value("2024/123456"))

        verify { aksesskontroll.autoriser(BEH_ID) }
    }

    @Test
    fun `placeholder-toggle av - begge endepunkter svarer 404`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER_DYNAMISK_PLACEHOLDER) } returns false

        mockMvc.perform(get(KATALOG_URL)).andExpect(status().isNotFound)
        mockMvc.perform(get(VERDIER_URL, BEH_ID)).andExpect(status().isNotFound)

        verify(exactly = 0) { placeholderService.hentKatalog() }
        verify(exactly = 0) { placeholderService.hentVerdier(any()) }
    }

    @Test
    fun `tekstblokker-toggle av - begge endepunkter svarer 404`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER) } returns false

        mockMvc.perform(get(KATALOG_URL)).andExpect(status().isNotFound)
        mockMvc.perform(get(VERDIER_URL, BEH_ID)).andExpect(status().isNotFound)

        verify(exactly = 0) { placeholderService.hentKatalog() }
        verify(exactly = 0) { placeholderService.hentVerdier(any()) }
    }

    @Test
    fun `uautorisert - svarer 403 og henter ikke verdier`() {
        every { aksesskontroll.autoriser(BEH_ID) } throws SikkerhetsbegrensningException("Ikke tilgang")

        mockMvc.perform(get(VERDIER_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden)

        verify(exactly = 0) { placeholderService.hentVerdier(any()) }
    }

    companion object {
        private const val KATALOG_URL = "/api/brev/placeholdere"
        private const val VERDIER_URL = "/api/behandlinger/{behandlingID}/placeholdere"
        private const val BEH_ID = 1234L
    }
}
