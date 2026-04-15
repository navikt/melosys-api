package no.nav.melosys.integrasjon.oppgave.konsument

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.readResourceAsString
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        OppgaveClientProducer::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveClientTest(
    @Autowired private val oppgaveClient: OppgaveClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
        oAuthMockServer.reset()
    }

    @Test
    fun `hentOppgave oppgave finnes verifiserer mapping`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
            )
        )

        val oppgave = oppgaveClient.hentOppgave("1")

        oppgave!!.run {
            id shouldBe "11519"
            tildeltEnhetsnr shouldBe "4530"
            journalpostId shouldBe "439654251"
            behandlesAvApplikasjon shouldBe "FS38"
            saksreferanse shouldBe "MEL-301"
            aktørId shouldBe "1332607802528"
            orgnr shouldBe "orgnr"
            tilordnetRessurs shouldBe "Z990757"
            beskrivelse shouldBe " "
            tema shouldBe "MED"
            behandlingstema shouldBe "ab0390"
            oppgavetype shouldBe "BEH_SED"
            versjon shouldBe 3
            prioritet shouldBe "NORM"
            status shouldBe "AAPNET"
            fristFerdigstillelse shouldBe LocalDate.parse("2019-12-26")
            aktivDato shouldBe LocalDate.parse("2019-10-03")
            opprettetTidspunkt shouldBe ZonedDateTime.parse("2019-10-03T10:27:26.566Z")
        }
    }

    @Test
    fun `hentOppgave oppgave finnes ikke 404 status kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString(OPPGAVE_FEILMELDING_JSON_PATH))
            )
        )

        val exception = shouldThrow<TekniskException> {
            oppgaveClient.hentOppgave("1")
        }

        exception.message shouldContain "Kall mot Oppgave feilet."
    }

    @Test
    fun `hentOppgaveListe mottar to oppgaver verifiserer mapping`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString(OPPGAVELIST_GET_JSON_PATH))
            )
        )

        val searchRequest = OppgaveSearchRequest.Builder("123").apply {
            medOppgaveTyper(arrayOf("BEH_SED", "BEH_SAK"))
            medAktørId("123")
            medBehandlingstema("ab2344")
            medBehandlingsType("ba432?")
            medBehandlesAvApplikasjon("FS38")
            medTema("MED", "UFM")
            medStatusKategori("AAPEN")
        }.build()

        val oppgaver = oppgaveClient.hentOppgaveListe(searchRequest)

        oppgaver.map { it.saksreferanse } shouldContainExactlyInAnyOrder listOf("MEL-301", "MEL-513")
    }

    @Test
    fun `opprettOppgave serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
            )
        )

        val request = OpprettOppgaveDto().apply {
            aktivDato = LocalDate.of(2024, 1, 15)
            aktørId = "1332607802528"
            orgnr = null
            behandlesAvApplikasjon = "FS38"
            behandlingstema = "ab0390"
            behandlingstype = null
            beskrivelse = "Test oppgave"
            fristFerdigstillelse = LocalDate.of(2024, 2, 1)
            journalpostId = "439654251"
            oppgavetype = "BEH_SED"
            prioritet = "NORM"
            saksreferanse = "MEL-123"
            tema = "MED"
            temagruppe = null
            tildeltEnhetsnr = "4530"
            tilordnetRessurs = null
            mappeId = null
        }

        oppgaveClient.opprettOppgave(request)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/oppgaver"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "aktivDato": "2024-01-15",
                            "aktoerId": "1332607802528",
                            "orgnr": null,
                            "behandlesAvApplikasjon": "FS38",
                            "behandlingstema": "ab0390",
                            "behandlingstype": null,
                            "beskrivelse": "Test oppgave",
                            "fristFerdigstillelse": "2024-02-01",
                            "journalpostId": "439654251",
                            "oppgavetype": "BEH_SED",
                            "prioritet": "NORM",
                            "saksreferanse": "MEL-123",
                            "tema": "MED",
                            "temagruppe": null,
                            "tildeltEnhetsnr": "4530",
                            "tilordnetRessurs": null,
                            "mappeId": null
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `oppdaterOppgave serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
            )
        )

        val request = OppgaveDto().apply {
            id = "11519"
            versjon = 3
            status = "AAPNET"
            aktivDato = LocalDate.of(2024, 1, 15)
            aktørId = "1332607802528"
            behandlesAvApplikasjon = "FS38"
            behandlingstema = "ab0390"
            beskrivelse = "Oppdatert beskrivelse"
            fristFerdigstillelse = LocalDate.of(2024, 2, 1)
            journalpostId = "439654251"
            oppgavetype = "BEH_SED"
            prioritet = "NORM"
            saksreferanse = "MEL-123"
            tema = "MED"
            tildeltEnhetsnr = "4530"
        }

        oppgaveClient.oppdaterOppgave(request)

        mockServer.verify(
            putRequestedFor(urlEqualTo("/api/v1/oppgaver/11519"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "id": "11519",
                            "versjon": 3,
                            "status": "AAPNET",
                            "aktivDato": "2024-01-15",
                            "aktoerId": "1332607802528",
                            "orgnr": null,
                            "behandlesAvApplikasjon": "FS38",
                            "behandlingstema": "ab0390",
                            "behandlingstype": null,
                            "beskrivelse": "Oppdatert beskrivelse",
                            "fristFerdigstillelse": "2024-02-01",
                            "journalpostId": "439654251",
                            "oppgavetype": "BEH_SED",
                            "prioritet": "NORM",
                            "saksreferanse": "MEL-123",
                            "tema": "MED",
                            "temagruppe": null,
                            "tildeltEnhetsnr": "4530",
                            "tilordnetRessurs": null,
                            "mappeId": null
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    companion object {
        private const val OPPGAVE_GET_JSON_PATH = "mock/oppgave/oppgave_get.json"
        private const val OPPGAVELIST_GET_JSON_PATH = "mock/oppgave/hentOppgaveListe_get.json"
        private const val OPPGAVE_FEILMELDING_JSON_PATH = "mock/oppgave/feil.json"
    }
}
