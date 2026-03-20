package no.nav.melosys.integrasjon.trygdeavgift

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
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.readResourceAsString
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        TrygdeavgiftClient::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrygdeavgiftClientTest(
    @Autowired private val trygdeavgiftClient: TrygdeavgiftClient,
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
    fun `beregnTrygdeavgift serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString("mock/trygdeavgift/trygdeavgift.json"))
            )
        )

        val request = TrygdeavgiftsberegningRequest(
            medlemskapsperioder = emptySet(),
            skatteforholdsperioder = emptySet(),
            inntektsperioder = emptyList(),
            foedselsdato = LocalDate.of(2000, 1, 1)
        )

        trygdeavgiftClient.beregnTrygdeavgift(request)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/trygdeavgift/v2/beregn"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "medlemskapsperioder": [],
                            "skatteforholdsperioder": [],
                            "inntektsperioder": [],
                            "foedselsdato": "2000-01-01"
                        }
                        """,
                        // TrygdeavgiftClient bruker sin egen WebClient uten JavaTimeModule,
                        // så LocalDate serialiseres som timestamp-array. Testen dokumenterer
                        // denne faktiske atferden som safety net for Spring Boot 4-oppgradering.
                        true, false
                    )
                )
        )
    }

    @Test
    fun `beregn trygdeavgift mapper response korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(readResourceAsString("mock/trygdeavgift/trygdeavgift.json"))
            )
        )

        val response = trygdeavgiftClient.beregnTrygdeavgift(lagTrygdeavgiftsberegningRequest())

        response[0].run {
            beregnetPeriode.sats shouldBe BigDecimal.valueOf(21.8)
            beregnetPeriode.månedsavgift shouldBe PengerDto(BigDecimal.valueOf(21800))
            component2() shouldNotBe null
        }
    }

    private fun lagTrygdeavgiftsberegningRequest() = TrygdeavgiftsberegningRequest(
        emptySet(),
        emptySet(),
        emptyList(),
        LocalDate.now().minusYears(20)
    )
}

