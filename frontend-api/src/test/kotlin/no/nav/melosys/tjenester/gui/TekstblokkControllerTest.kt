package no.nav.melosys.tjenester.gui

import java.time.Instant
import java.time.LocalDateTime

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.Unleash
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.tekstblokk.Endringstype
import no.nav.melosys.service.tekstblokk.TekstblokkHistorikkService
import no.nav.melosys.service.tekstblokk.TekstblokkService
import no.nav.melosys.service.tekstblokk.TekstblokkVersjon
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
    private lateinit var tekstblokkHistorikkService: TekstblokkHistorikkService

    @MockkBean
    private lateinit var unleash: Unleash

    @BeforeEach
    fun setUp() {
        every { unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER) } returns true
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns true
    }

    @Test
    fun `henting av en blokk leverer avgrensningene som kode-strenger`() {
        every { tekstblokkService.hent(1L, true) } returns tekstblokk()

        mockMvc.perform(get("$BASE_URL/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sakstyper", contains("EU_EOS", "TRYGDEAVTALE")))
            .andExpect(jsonPath("$.behandlingstemaer", contains("ARBEID_FLERE_LAND")))
    }

    @Test
    fun `oversikten leverer avgrensningene som kode-strenger`() {
        every { tekstblokkService.hentAlleOversikter(null, true) } returns listOf(oversikt())

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sakstyper", contains("FTRL")))
            .andExpect(jsonPath("$[0].behandlingstemaer", contains("PENSJONIST", "YRKESAKTIV")))
    }

    @Test
    fun `blokk uten avgrensning leverer tomme lister, ikke null`() {
        every { tekstblokkService.hent(1L, true) } returns tekstblokk(
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

    @Test
    fun `tom streng i kodelisten gir 400, ikke 500`() {
        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(
                """{"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK","sakstyper":[""]}""",
            ),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `status leveres som streng i baade detalj og oversikt`() {
        every { tekstblokkService.hent(1L, true) } returns tekstblokk(status = TekstblokkStatus.UTKAST)
        every { tekstblokkService.hentAlleOversikter(null, true) } returns listOf(oversikt(TekstblokkStatus.UTKAST))

        mockMvc.perform(get("$BASE_URL/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UTKAST"))

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("UTKAST"))
    }

    @Test
    fun `saksbehandler uten admin-toggle faar oversikten uten utkast`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns false
        every { tekstblokkService.hentAlleOversikter(null, false) } returns emptyList()

        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        verify { tekstblokkService.hentAlleOversikter(null, false) }
    }

    @Test
    fun `saksbehandler uten admin-toggle spoer detaljvisningen uten utkast`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns false
        every { tekstblokkService.hent(1L, false) } returns tekstblokk()

        mockMvc.perform(get("$BASE_URL/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)

        verify { tekstblokkService.hent(1L, false) }
    }

    @Test
    fun `status tas imot som enum ved opprettelse`() {
        val input = slot<TekstblokkService.Input>()
        every { tekstblokkService.opprett(capture(input)) } returns tekstblokk()

        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                .content("""{"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK","status":"UTKAST"}"""),
        ).andExpect(status().isOk)

        input.captured.status shouldBe TekstblokkStatus.UTKAST
    }

    @Test
    fun `opprettelse uten status blir publisert`() {
        val input = slot<TekstblokkService.Input>()
        every { tekstblokkService.opprett(capture(input)) } returns tekstblokk()

        mockMvc.perform(
            post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                .content("""{"tittel":"Tittel","innhold":"<p>Tekst</p>","type":"TEKSTBLOKK"}"""),
        ).andExpect(status().isOk)

        input.captured.status shouldBe TekstblokkStatus.PUBLISERT
    }

    @Test
    fun `publiser-endepunktet returnerer den publiserte blokken`() {
        every { tekstblokkService.publiser(1L) } returns tekstblokk()

        mockMvc.perform(post("$BASE_URL/{id}/publiser", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PUBLISERT"))
    }

    @Test
    fun `publiser-endepunktet krever admin-toggle`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns false

        mockMvc.perform(post("$BASE_URL/{id}/publiser", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `historikk leveres med versjonsnummer og endringstype`() {
        every { tekstblokkHistorikkService.hentHistorikk(1L) } returns listOf(
            TekstblokkVersjon(
                versjon = 1,
                gyldigFra = LocalDateTime.of(2026, 7, 31, 12, 0),
                gyldigTil = null,
                endretAv = IDENT,
                endretAvNavn = "Margareth Bjørgum",
                endringstype = Endringstype.OPPRETTET,
                tittel = "Tittel",
                innhold = "<p>Tekst</p>",
            ),
        )

        mockMvc.perform(get("$BASE_URL/{id}/historikk", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].versjon").value(1))
            .andExpect(jsonPath("$[0].endringstype").value("OPPRETTET"))
            .andExpect(jsonPath("$[0].endretAvNavn").value("Margareth Bjørgum"))
            .andExpect(jsonPath("$[0].gyldigTil").doesNotExist())
    }

    @Test
    fun `historikk krever admin-toggle`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON) } returns false

        mockMvc.perform(get("$BASE_URL/{id}/historikk", 1L).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden)
    }

    private fun tekstblokk(
        sakstyper: Set<Sakstyper> = setOf(Sakstyper.TRYGDEAVTALE, Sakstyper.EU_EOS),
        behandlingstemaer: Set<Behandlingstema> = setOf(Behandlingstema.ARBEID_FLERE_LAND),
        status: TekstblokkStatus = TekstblokkStatus.PUBLISERT,
    ) = Tekstblokk(
        id = 1L,
        tittel = "Tittel",
        innhold = "<p>Tekst</p>",
        type = TekstblokkType.TEKSTBLOKK,
        status = status,
        sakstyper = sakstyper.toMutableSet(),
        behandlingstemaer = behandlingstemaer.toMutableSet(),
    ).apply {
        registrertDato = NÅ
        registrertAv = IDENT
        endretDato = NÅ
        endretAv = IDENT
    }

    private fun oversikt(status: TekstblokkStatus = TekstblokkStatus.PUBLISERT) = TekstblokkOversikt(
        id = 1L,
        tittel = "Tittel",
        innhold = "<p>Tekst</p>",
        type = TekstblokkType.TEKSTBLOKK,
        status = status,
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
