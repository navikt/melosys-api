package no.nav.melosys.itest

import tools.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsnotatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

/**
 * Går hele veien fra REST-endepunkt via service og JPA til Oracle, og tilbake:
 * notatet lagres på behandlingen angitt av behandlingId (eller den backend utleder),
 * og hentes deretter for hele fagsaken med riktig behandlingId, type og redigerbar-flagg.
 *
 * Enhetstestene for [no.nav.melosys.service.BehandlingsnotatService] dekker valg av behandling
 * med mocket fagsak. Denne testen beviser at koblingen faktisk persisteres og kommer tilbake
 * gjennom fagsakens behandlinger slik frontend leser den.
 */
@AutoConfigureMockMvc
@Import(BehandlingsnotatIT.TestConfig::class)
class BehandlingsnotatIT @Autowired constructor(
    private val mockMvc: MockMvc,
    private val mockOAuth2Server: MockOAuth2Server,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsnotatRepository: BehandlingsnotatRepository,
) : ComponentTestBase() {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun aksesskontroll(): Aksesskontroll = mockk(relaxed = true)
    }

    private val saksnummer = "MEL-NOTAT-1"
    private val saksbehandler = "Z123456"

    /**
     * Testprofilen har bare issuer «isso», mens SpringSubjectHandler leser NAVident fra «aad»-token.
     * Uten mock blir notatet registrert på systembrukeren MELOSYS, og redigerbar-flagget
     * (registrertAv == innlogget bruker) kan ikke verifiseres. Samme mønster som EndreAktoerIdIT.
     */
    @BeforeEach
    fun settInnloggetSaksbehandler() {
        val subjectHandler = mockk<SpringSubjectHandler>(relaxed = true)
        every { subjectHandler.userID } returns saksbehandler
        SubjectHandler.set(subjectHandler)
    }

    @AfterEach
    fun resetSubjectHandler() {
        SubjectHandler.set(SpringSubjectHandler(SpringTokenValidationContextHolder()))
    }

    @Test
    fun `notat med behandlingId lagres på årsavregningen og hentes for hele fagsaken sammen med notat fra førstegangsbehandlingen`() {
        val fagsak = lagFagsak()
        val førstegang = lagBehandling(fagsak, Behandlingstyper.FØRSTEGANG)
        val årsavregning = lagBehandling(fagsak, Behandlingstyper.ÅRSAVREGNING)

        // Uten behandlingId: bakoverkompatibel oppførsel, ordinær aktiv behandling foretrekkes
        val notatUtenId = opprettNotat("Notat fra førstegangsbehandlingen", behandlingId = null)
        notatUtenId["behandlingId"].asLong() shouldBe førstegang.id

        // Med behandlingId: notatet skal på årsavregningen selv om førstegangsbehandlingen er aktiv
        val notatMedId = opprettNotat("Sjekket 25 %-regelen i årsavregningen", behandlingId = årsavregning.id)
        notatMedId["behandlingId"].asLong() shouldBe årsavregning.id
        notatMedId["behandlingstypeKode"].asText() shouldBe Behandlingstyper.ÅRSAVREGNING.kode

        // Persistert kobling, ikke bare DTO-mapping av objektet i minnet
        behandlingsnotatRepository.findById(notatMedId["notatId"].asLong()).get().behandling.id shouldBe årsavregning.id

        val hentet = hentNotater()
        hentet.map { it["notatId"].asLong() } shouldContainExactlyInAnyOrder listOf(
            notatUtenId["notatId"].asLong(),
            notatMedId["notatId"].asLong(),
        )
        hentet.single { it["behandlingId"].asLong() == årsavregning.id }.let {
            it["tekst"].asText() shouldBe "Sjekket 25 %-regelen i årsavregningen"
            it["behandlingstypeKode"].asText() shouldBe Behandlingstyper.ÅRSAVREGNING.kode
            it["behandlingstemaKode"].asText() shouldBe Behandlingstema.YRKESAKTIV.kode
            it["redigerbar"].asBoolean() shouldBe true
        }
        hentet.single { it["behandlingId"].asLong() == førstegang.id }.let {
            it["behandlingstypeKode"].asText() shouldBe Behandlingstyper.FØRSTEGANG.kode
            it["redigerbar"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `notat fra avsluttet førstegangsbehandling vises fortsatt når årsavregning er eneste aktive, og nytt notat havner på årsavregningen`() {
        val fagsak = lagFagsak()
        val førstegang = lagBehandling(fagsak, Behandlingstyper.FØRSTEGANG)
        val notatFraFørstegang = opprettNotat("Skrevet mens førstegangsbehandlingen var aktiv", behandlingId = null)
        notatFraFørstegang["behandlingId"].asLong() shouldBe førstegang.id

        behandlingRepository.save(førstegang.apply { status = Behandlingsstatus.AVSLUTTET })
        val årsavregning = lagBehandling(fagsak, Behandlingstyper.ÅRSAVREGNING)

        // Feilen Annette traff: uten behandlingId ga dette "Fagsak har ingen aktive behandlinger"
        val notatPåÅrsavregning = opprettNotat("Skrevet i årsavregningen", behandlingId = null)
        notatPåÅrsavregning["behandlingId"].asLong() shouldBe årsavregning.id

        val hentet = hentNotater()
        hentet.size shouldBe 2
        hentet.single { it["behandlingId"].asLong() == førstegang.id }.let {
            it["tekst"].asText() shouldBe "Skrevet mens førstegangsbehandlingen var aktiv"
            it["behandlingstypeKode"].asText() shouldBe Behandlingstyper.FØRSTEGANG.kode
            it["redigerbar"].asBoolean() shouldBe false
        }
        hentet.single { it["behandlingId"].asLong() == årsavregning.id }.let {
            it["behandlingstypeKode"].asText() shouldBe Behandlingstyper.ÅRSAVREGNING.kode
            it["redigerbar"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `flere aktive årsavregninger krever behandlingId`() {
        val fagsak = lagFagsak()
        val årsavregning2024 = lagBehandling(fagsak, Behandlingstyper.ÅRSAVREGNING)
        val årsavregning2025 = lagBehandling(fagsak, Behandlingstyper.ÅRSAVREGNING)

        val feil = mockMvc.perform(postNotat("Tvetydig", behandlingId = null))
            .andExpect(status().isBadRequest)
            .andReturn().response.contentAsString
        feil shouldContain "behandlingId må oppgis"

        opprettNotat("Notat for 2025", behandlingId = årsavregning2025.id)["behandlingId"].asLong() shouldBe årsavregning2025.id

        hentNotater().single()["behandlingId"].asLong() shouldBe årsavregning2025.id
        behandlingsnotatRepository.findAll().none { it.behandling.id == årsavregning2024.id } shouldBe true
    }

    private fun lagFagsak(): Fagsak = fagsakRepository.save(
        Fagsak(
            saksnummer = saksnummer,
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET,
        )
    )

    private fun lagBehandling(fagsak: Fagsak, behandlingstype: Behandlingstyper): Behandling =
        behandlingRepository.save(
            Behandling.forTest {
                this.fagsak = fagsak
                status = Behandlingsstatus.UNDER_BEHANDLING
                type = behandlingstype
                tema = Behandlingstema.YRKESAKTIV
                behandlingsfrist = LocalDate.now()
            }
        )

    private fun opprettNotat(tekst: String, behandlingId: Long?): JsonNode =
        mockMvc.perform(postNotat(tekst, behandlingId))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
            .let(objectMapper::readTree)

    private fun postNotat(tekst: String, behandlingId: Long?) =
        post("/api/fagsaker/{saksnummer}/notater", saksnummer)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(BehandlingsnotatPost(tekst, behandlingId)))

    private fun hentNotater(): List<JsonNode> =
        mockMvc.perform(
            get("/api/fagsaker/{saksnummer}/notater", saksnummer)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
            .let(objectMapper::readTree)
            .toList()

    private fun hentBearerToken(): String = mockOAuth2Server.issueToken(
        issuerId = "issuer1",
        subject = "testbruker",
        audience = "dumbdumb",
        claims = mapOf(
            "oid" to "test-oid",
            "azp" to "test-azp",
            "NAVident" to saksbehandler,
        )
    ).serialize()

    /** Speiler BehandlingsnotatPostDto uten å avhenge av frontend-api sin klasse. */
    private data class BehandlingsnotatPost(val tekst: String, val behandlingId: Long?)
}
