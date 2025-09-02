package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(controllers = [KontaktopplysningController::class])
class KontaktopplysningControllerTest {

    @MockkBean
    private lateinit var kontaktopplysningService: KontaktopplysningService
    
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `hentKontaktopplysning skal kalle på service når objekt finnes`() {
        every { aksesskontroll.autoriserSakstilgang(SAK_NUMMER) } returns Unit
        every { kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER) } returns Optional.of(Kontaktopplysning())


        mockMvc.perform(
            get("$BASE_URL/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
    }

    @Test
    fun `hentKontaktopplysning skal kalle på service når objekt ikke finnes`() {
        every { aksesskontroll.autoriserSakstilgang(SAK_NUMMER) } returns Unit
        every { kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER) } returns Optional.empty()


        mockMvc.perform(
            get("$BASE_URL/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound())


        verify { kontaktopplysningService.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER) }
    }

    @Test
    fun `lagKontaktopplysning skal kalle på service`() {
        every { aksesskontroll.autoriserSakstilgang(SAK_NUMMER) } returns Unit
        val kontaktopplysning = Kontaktopplysning.av(ORG_NUMMER, KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon(), KONTAKT_INFO.kontaktorgnr())
        every { kontaktopplysningService.lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO.kontaktorgnr(), KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon()) } returns kontaktopplysning


        mockMvc.perform(
            post("$BASE_URL/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(KONTAKT_INFO))
        ).andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(kontaktopplysning, Kontaktopplysning::class.java))


        verify(exactly = 1) { 
            kontaktopplysningService.lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO.kontaktorgnr(), KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon()) 
        }
    }

    @Test
    fun `slettKontaktopplysning skal kalle deleteById med gitt saksnummer og orgnummer`() {
        every { aksesskontroll.autoriserSakstilgang(SAK_NUMMER) } returns Unit
        every { kontaktopplysningService.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER) } returns Unit


        mockMvc.perform(
            delete("$BASE_URL/{saksnummer}/kontaktopplysninger/{orgnr}", SAK_NUMMER, ORG_NUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent())


        verify { kontaktopplysningService.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER) }
    }

    companion object {
        private const val BASE_URL = "/api/fagsaker"
        private const val SAK_NUMMER = "MEL-1"
        private const val ORG_NUMMER = "999"
        private val KONTAKT_INFO = KontaktInfoDto("kontaktnavn", "kontaktorgnr", "kontakttelefon")
    }
}