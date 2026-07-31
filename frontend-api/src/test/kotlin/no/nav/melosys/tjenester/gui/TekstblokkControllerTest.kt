package no.nav.melosys.tjenester.gui

import java.time.Instant

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.Unleash
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.tekstblokk.TekstblokkService
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [TekstblokkController::class])
class TekstblokkControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    private lateinit var tekstblokkService: TekstblokkService

    @MockkBean
    private lateinit var unleash: Unleash

    @BeforeEach
    fun setUp() {
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER) } returns true
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns true
    }

    @Test
    fun `henting av en blokk leverer avgrensningene som kode-strenger`() {
        every { tekstblokkService.hent(1L) } returns tekstblokk()

        mockMvc.perform(get("$BASE_URL/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sakstyper", contains("EU_EOS", "TRYGDEAVTALE")))
            .andExpect(jsonPath("$.behandlingstemaer", contains("ARBEID_FLERE_LAND")))
    }

    @Test
    fun `oversikten leverer avgrensningene som kode-strenger`() {
        every { tekstblokkService.hentAlleOversikter(null) } returns listOf(oversikt())

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sakstyper", contains("FTRL")))
            .andExpect(jsonPath("$[0].behandlingstemaer", contains("PENSJONIST", "YRKESAKTIV")))
    }

    @Test
    fun `blokk uten avgrensning leverer tomme lister, ikke null`() {
        every { tekstblokkService.hent(1L) } returns tekstblokk(
            sakstyper = emptySet(),
            behandlingstemaer = emptySet(),
        )

        mockMvc.perform(get("$BASE_URL/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sakstyper.length()").value(0))
            .andExpect(jsonPath("$.behandlingstemaer.length()").value(0))
    }

    @Test
    fun `opprettelse tar imot rene koder og gir dem videre som enum`() {
        val input = slot<TekstblokkService.Input>()
        every { tekstblokkService.opprett(capture(input)) } returns tekstblokk()

        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(
                """
                {"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK",
                 "sakstyper":["EU_EOS"],"behandlingstemaer":["ARBEID_FLERE_LAND"]}
                """.trimIndent(),
            ),
        ).andExpect(status().isOk)

        input.captured.sakstyper shouldBe listOf(Sakstyper.EU_EOS)
        input.captured.behandlingstemaer shouldBe listOf(Behandlingstema.ARBEID_FLERE_LAND)
    }

    @Test
    fun `opprettelse uten avgrensningsfelter er gyldig`() {
        val input = slot<TekstblokkService.Input>()
        every { tekstblokkService.opprett(capture(input)) } returns tekstblokk()

        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                .content("""{"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK"}"""),
        ).andExpect(status().isOk)

        input.captured.sakstyper.shouldBeNull()
        input.captured.behandlingstemaer.shouldBeNull()
    }

    @Test
    fun `ukjent kode gir 400`() {
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(
                """{"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK","sakstyper":["FINNES_IKKE"]}""",
            ),
        ).andExpect(status().isBadRequest)
    }

    private fun tekstblokk(
        sakstyper: Set<Sakstyper> = setOf(Sakstyper.TRYGDEAVTALE, Sakstyper.EU_EOS),
        behandlingstemaer: Set<Behandlingstema> = setOf(Behandlingstema.ARBEID_FLERE_LAND),
    ) = Tekstblokk(
        id = 1L,
        tittel = "Tittel",
        innhold = "<p>Tekst</p>",
        type = TekstblokkType.TEKSTBLOKK,
        sakstyper = sakstyper.toMutableSet(),
        behandlingstemaer = behandlingstemaer.toMutableSet(),
    ).apply {
        registrertDato = NÅ
        registrertAv = IDENT
        endretDato = NÅ
        endretAv = IDENT
    }

    private fun oversikt() = TekstblokkOversikt(
        id = 1L,
        tittel = "Tittel",
        innhold = "<p>Tekst</p>",
        type = TekstblokkType.TEKSTBLOKK,
        endretDato = NÅ,
        endretAv = IDENT,
        endretAvNavn = null,
    ).apply {
        sakstyper = setOf(Sakstyper.FTRL)
        behandlingstemaer = setOf(Behandlingstema.YRKESAKTIV, Behandlingstema.PENSJONIST)
    }

    private companion object {
        const val BASE_URL = "/api/brev/tekstblokker"
        const val IDENT = "Z224234"
        val NÅ: Instant = Instant.parse("2026-07-31T10:00:00Z")
    }
}
