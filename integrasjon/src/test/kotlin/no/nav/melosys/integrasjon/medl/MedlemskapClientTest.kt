package no.nav.melosys.integrasjon.medl

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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        MedlemskapClientConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedlemskapClientTest(
    @Autowired private val medlemskapClient: MedlemskapClient,
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
    fun `skal hente medlemskapsperiodeliste og serialisere soek-request korrekt`() {
        val fnr = "12345678990"
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 6, 30)

        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")
                )
        )

        medlemskapClient.hentPeriodeListe(fnr, fom, tom).shouldBeEmpty()

        mockServer.verify(
            postRequestedFor(urlEqualTo("/rest/v1/periode/soek"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "personident": "12345678990",
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-06-30",
                            "inkluderSporingsinfo": true,
                            "ekskluderKilder": []
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `skal hente en medlemskapsperiode`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"unntakId": 123}""")
                )
        )

        medlemskapClient.hentPeriode("123") shouldNotBe null
    }

    @Test
    fun `skal kaste RuntimeException ved oppdatering`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withBody("Validering feilet")
                )
        )

        val exception = shouldThrow<RuntimeException> {
            medlemskapClient.oppdaterPeriode(MedlemskapsunntakForPut())
        }

        exception.message shouldContain "400"
    }

    @Test
    fun `oppdaterPeriode serialiserer request body korrekt`() {
        val request = MedlemskapsunntakForPut(
            unntakId = 12345L,
            fraOgMed = LocalDate.of(2023, 1, 1),
            tilOgMed = LocalDate.of(2023, 12, 31),
            status = "UENDRET",
        )

        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"unntakId": 12345}""")
                )
        )

        medlemskapClient.oppdaterPeriode(request) shouldNotBe null

        mockServer.verify(
            putRequestedFor(urlEqualTo("/api/v1/medlemskapsunntak"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "unntakId": 12345,
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-12-31",
                            "status": "UENDRET",
                            "statusaarsak": null,
                            "dekning": null,
                            "lovvalgsland": null,
                            "lovvalg": null,
                            "grunnlag": null,
                            "sporingsinformasjon": null
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `opprettPeriode serialiserer request body korrekt`() {
        val request = MedlemskapsunntakForPost(
            ident = "12345678901",
            fraOgMed = LocalDate.of(2023, 1, 1),
            tilOgMed = LocalDate.of(2023, 12, 31),
            status = "UNNTAK",
            lovvalgsland = "SWE",
            lovvalg = "NASJONAL",
        )

        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"unntakId": 999}""")
                )
        )

        medlemskapClient.opprettPeriode(request) shouldNotBe null

        mockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/medlemskapsunntak"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "ident": "12345678901",
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-12-31",
                            "status": "UNNTAK",
                            "statusaarsak": null,
                            "dekning": null,
                            "lovvalgsland": "SWE",
                            "lovvalg": "NASJONAL",
                            "grunnlag": null,
                            "sporingsinformasjon": null
                        }
                        """,
                        true, false
                    )
                )
        )
    }
}

