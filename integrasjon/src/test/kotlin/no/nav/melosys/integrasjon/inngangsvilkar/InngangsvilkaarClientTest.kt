package no.nav.melosys.integrasjon.inngangsvilkar

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
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
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        CorrelationIdOutgoingFilter::class,
        InngangsvilkarClientConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InngangsvilkaarClientTest(
    @Autowired private val inngangsvilkaarClient: InngangsvilkaarClient,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
    }

    @Test
    fun `vurderInngangsvilkaar serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"kvalifisererForEf883_2004": true, "feilmeldinger": []}""")
                )
        )

        val request = VurderInngangsvilkaarRequest(
            statsborgerskap = setOf(Land.NORGE),
            arbeidsland = setOf(Land.SVERIGE),
            flereLandUkjentHvilke = false,
            periode = Periode(LocalDate.of(2023, 1, 15), LocalDate.of(2023, 6, 30))
        )

        val response = inngangsvilkaarClient.vurderInngangsvilkår(request)

        response.kvalifisererForEf883_2004 shouldBe true

        mockServer.verify(
            postRequestedFor(urlEqualTo("/inngangsvilkaar"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "statsborgerskap": ["NOR"],
                            "arbeidsland": ["SWE"],
                            "flereLandUkjentHvilke": false,
                            "periode": {
                                "fom": "2023-01-15",
                                "tom": "2023-06-30"
                            }
                        }
                        """,
                        true, false
                    )
                )
        )
    }
}

