package no.nav.melosys.integrasjon.popp

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.nio.charset.StandardCharsets
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        PoppInntektClientConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PoppInntektClientTest(
    @Autowired private val poppInntektClient: PoppInntektClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
) {

    private val processUUID = UUID.randomUUID()
    private val mockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        mockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        oAuthMockServer.stop()
    }

    @BeforeEach
    fun before() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        mockServer.resetAll()
        oAuthMockServer.reset()
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @Test
    fun `hentInntekt - Skatt-only - returnerer alle perioder med kilde SKATT`() {
        stubOk("mock/popp/poppSkattOnly.json")

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = FNR, fomAr = 2020, tomAr = 2024)
        )

        respons.inntekter.shouldHaveSize(2)
        respons.inntekter.all { it.kilde == "SKATT" } shouldBe true
        respons.inntekter.first().belop shouldBe 540000L
        respons.inntekter.first().inntektAr shouldBe 2024
    }

    @Test
    fun `hentInntekt - Skatt og Avgiftssystemet for samme ar - returnerer to rader for aret`() {
        stubOk("mock/popp/poppSkattOgAvgiftssystemet.json")

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = FNR, fomAr = 2020, tomAr = 2024)
        )

        respons.inntekter.shouldHaveSize(2)
        respons.inntekter.filter { it.inntektAr == 2024 }.shouldHaveSize(2)
        respons.inntekter.map { it.kilde }.toSet() shouldBe setOf("SKATT", "AVGIFTSSYSTEMET")
    }

    @Test
    fun `hentInntekt - tom liste - returnerer tom liste`() {
        stubOk("mock/popp/poppTom.json")

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = FNR, fomAr = 2020, tomAr = 2024)
        )

        respons.inntekter.shouldBeEmpty()
    }

    @Test
    fun `hentInntekt - 404 fra POPP - mappes til tom liste`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"message":"Person ikke funnet","code":"PERSON_IKKE_FUNNET"}""")
            )
        )

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = FNR, fomAr = 2020, tomAr = 2024)
        )

        respons.inntekter.shouldBeEmpty()
    }

    @Test
    fun `hentInntekt - serialiserer request body korrekt og setter Authorization-header`() {
        stubOk("mock/popp/poppTom.json")

        poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = FNR, fomAr = 2020, tomAr = 2024)
        )

        mockServer.verify(
            postRequestedFor(urlEqualTo("/popp/inntekt/hent"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """{"fnr":"$FNR","fomAr":2020,"tomAr":2024}""",
                        true,
                        true
                    )
                )
        )
    }

    private fun stubOk(resource: String) {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(lesRessurs(resource))
            )
        )
    }

    private fun lesRessurs(path: String): String =
        PoppInntektClientTest::class.java.classLoader.getResource(path)
            ?.readText(StandardCharsets.UTF_8)
            ?: throw IkkeFunnetException("Fant ikke $path")

    companion object {
        private const val FNR = "12345678901"
    }
}
