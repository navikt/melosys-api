package no.nav.melosys.integrasjon.ereg.organisasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.nio.charset.StandardCharsets
import java.util.*

@Import(
    CorrelationIdOutgoingFilter::class,

    OrganisasjonRestConsumerConfig::class,
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisasjonRestConsumerTest(
    @Autowired private val organisasjonRestConsumer: OrganisasjonRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {


    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
    }

    @BeforeEach
    fun before() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.resetAll()
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }

    @Test
    fun `hent organisasjon av type JuridiskEnhet`() {
        val orgnummer = "928497704"
        lagStub(orgnummer)

        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(
            OrganisasjonRequest(orgnummer = orgnummer)
        )

        println(organisasjon.javaClass.simpleName)

        (organisasjon as OrganisasjonResponse.JuridiskEnhet).juridiskEnhetDetaljer!!.run {
            println(enhetstype)
        }

        println(organisasjon.toJsonNode.toPrettyString())
    }

    @Test
    fun `hent organisasjon av type Virksomhet`() {
        val orgnummer = "901851573"
        lagStub(orgnummer)

        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(
            OrganisasjonRequest(orgnummer = orgnummer)
        )

        println(organisasjon.javaClass.simpleName)

        (organisasjon as OrganisasjonResponse.Virksomhet).virksomhetDetaljer!!.run {
            println(enhetstype)
        }
    }

    @Test
    fun `hent organisasjon av type Organisasjonsledd`() {
        val orgnummer = "974774577"
        lagStub(orgnummer)

        val organisasjon = organisasjonRestConsumer.hentOrganisasjon(
            OrganisasjonRequest(orgnummer = orgnummer)
        )

        println(organisasjon.javaClass.simpleName)

        (organisasjon as OrganisasjonResponse.Organisasjonsledd).organisasjonsleddDetaljer!!.run {
            println("enhetstype:$enhetstype sektorkode:$sektorkode")
        }
    }

    private fun lagStub(orgnummer: String) {
        val fil = "mock/organisasjon/$orgnummer.json"
        val jsonData = OrganisasjonRestConsumerTest::class.java.classLoader.getResource(fil)
            ?.readText(StandardCharsets.UTF_8) ?: throw TekniskException("Fant ikke $fil")
        serviceUnderTestMockServer.stubFor(
            WireMock.get("/ereg/v2/organisasjon/$orgnummer")
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonData)
                )
        )
    }
}
