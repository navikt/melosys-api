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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.tilgang.ThreadLocalAccessInfo
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

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter.shouldNotBeNull()
        respons.inntekter!!.shouldHaveSize(2)
        respons.inntekter!!.all { it.kilde == "SKATT" } shouldBe true
    }

    @Test
    fun `hentInntekt - Skatt og Avgiftssystemet for samme ar - returnerer to rader for aret`() {
        stubOk("mock/popp/poppSkattOgAvgiftssystemet.json")

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter.shouldNotBeNull()
        respons.inntekter!!.shouldHaveSize(2)
        respons.inntekter!!.map { it.kilde }.toSet() shouldBe setOf("SKATT", "AVGIFTSSYSTEMET")
    }

    @Test
    fun `hentInntekt - tom liste - returnerer tom liste`() {
        stubOk("mock/popp/poppTom.json")

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter.shouldNotBeNull()
        respons.inntekter!!.shouldBeEmpty()
    }

    @Test
    fun `hentInntekt - 404 med PERSON_IKKE_FUNNET-body - mappes til tom liste`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"message":"Person ikke funnet","code":"PERSON_IKKE_FUNNET"}""")
            )
        )

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter shouldBe emptyList()
    }

    @Test
    fun `hentInntekt - 404 uten PERSON_IKKE_FUNNET-body - kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"message":"Endepunkt ikke funnet"}""")
            )
        )

        shouldThrow<TekniskException> { poppInntektClient.hentInntekt(REQUEST) }
    }

    @Test
    fun `hentInntekt - inntekter null i body - tolkes som tom liste`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"inntekter": null}""")
            )
        )

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter shouldBe emptyList()
    }

    @Test
    fun `hentInntekt - tom changeStamp-dato i body - deserialiseres uten feil`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        """{"inntekter":[{"inntektAr":2024,"kilde":"SKATT","inntektType":"FL_PGI_LOENN","belop":540000,"changeStamp":{"createdDate":"","updatedDate":""}}]}"""
                    )
            )
        )

        val respons = poppInntektClient.hentInntekt(REQUEST)

        respons.inntekter!!.shouldHaveSize(1)
    }

    @Test
    fun `hentInntekt - 500 med fnr i body - kaster TekniskException uten fnr i melding`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(500)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("""{"message":"Intern feil for fnr $FNR"}""")
            )
        )

        val ex = shouldThrow<TekniskException> { poppInntektClient.hentInntekt(REQUEST) }

        // POPP-feilrespons kan inneholde fnr; det skal kun til team-logs, aldri i exception-meldingen (eksponeres i HTTP-respons)
        ex.message!! shouldNotContain FNR
    }

    @Test
    fun `hentInntekt - 200 med tom body - kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody("")
            )
        )

        shouldThrow<TekniskException> { poppInntektClient.hentInntekt(REQUEST) }
    }

    @Test
    fun `hentInntekt - serialiserer request body korrekt`() {
        stubOk("mock/popp/poppTom.json")

        poppInntektClient.hentInntekt(REQUEST)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/popp/inntekt/hentgrunnlag"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """{"fnr":"$FNR","fomAr":2020,"tomAr":2024}""",
                        true,
                        false
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

    private fun <T : Any> T?.shouldNotBeNull(): T = this ?: error("Expected non-null value")

    companion object {
        private const val FNR = "12345678901"
        private val REQUEST = PoppHentInntektRequest(
            fnr = FNR,
            fomAr = 2020,
            tomAr = 2024,
        )
    }
}
