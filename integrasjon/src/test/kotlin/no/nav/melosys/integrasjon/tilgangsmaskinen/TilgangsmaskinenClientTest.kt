package no.nav.melosys.integrasjon.tilgangsmaskinen

import tools.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.AvvisningsKode
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.TilgangsmaskinenProblemDetail
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
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        TilgangsmaskinenClientProducer::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilgangsmaskinenClientTest(
    @Autowired private val tilgangsmaskinenClient: TilgangsmaskinenClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))
    private val objectMapper = JsonMapper.builder().build()

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
    fun `sjekkTilgang med kjerne regeltype serialiserer fnr korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse().withStatus(204)
            )
        )

        val harTilgang = tilgangsmaskinenClient.sjekkTilgang("12345678901", RegelType.KJERNE_REGELTYPE)

        harTilgang shouldBe true

        mockServer.verify(
            postRequestedFor(urlEqualTo("/tilgangsmaskinen/api/v1/kjerne"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalTo("12345678901"))
        )
    }

    @Test
    fun `sjekkTilgang med komplett regeltype bruker riktig endepunkt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse().withStatus(204)
            )
        )

        val harTilgang = tilgangsmaskinenClient.sjekkTilgang("12345678901", RegelType.KOMPLETT_REGELTYPE)

        harTilgang shouldBe true

        mockServer.verify(
            postRequestedFor(urlEqualTo("/tilgangsmaskinen/api/v1/komplett"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalTo("12345678901"))
        )
    }

    @Test
    fun `sjekkTilgang kaster TilgangsmaskinenException ved 403 respons`() {
        val problemDetail = TilgangsmaskinenProblemDetail(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            title = AvvisningsKode.AVVIST_STRENGT_FORTROLIG_ADRESSE,
            status = 403,
            instance = "Z990883/03508331575",
            brukerIdent = "03508331575",
            navIdent = "Z990883",
            traceId = "444290be30ed4fdd9a849654bad9dc1b",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            kanOverstyres = false
        )

        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(problemDetail))
            )
        )

        // errorFilter i TilgangsmaskinenClientProducer fanger 403 som TekniskException
        // før sjekkTilgang() sin catch-blokk kjøres, og wrapper det i TilgangsmaskinenException.
        // Testen dokumenterer faktisk nåværende atferd som safety net for Spring Boot 4-oppgradering.
        shouldThrow<TilgangsmaskinenException> {
            tilgangsmaskinenClient.sjekkTilgang("03508331575")
        }
    }

    @Test
    fun `sjekkTilgang kaster exception ved 404 respons`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"message\": \"Person ikke funnet\"}")
            )
        )

        shouldThrow<TilgangsmaskinenException> {
            tilgangsmaskinenClient.sjekkTilgang("12345678901")
        }
    }

    @Test
    fun `sjekkTilgang kaster exception ved 500 respons`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"error\": \"Internal Server Error\"}")
            )
        )

        shouldThrow<TilgangsmaskinenException> {
            tilgangsmaskinenClient.sjekkTilgang("12345678901")
        }
    }
}

