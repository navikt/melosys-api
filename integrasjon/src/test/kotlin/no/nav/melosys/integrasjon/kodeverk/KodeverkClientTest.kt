package no.nav.melosys.integrasjon.kodeverk

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.kodeverk.impl.KodeverkClient
import no.nav.melosys.integrasjon.kodeverk.impl.KodeverkClientConfig
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

/**
 * Tester at KodeverkClient:
 * - Kaller korrekt URL med riktige query-parametere
 * - Sender obligatoriske Nav-headere (Nav-Consumer-Id)
 * - Bruker Spring-konfigurerte beans — samme oppsett som produksjonskode
 * - Deserialiserer FellesKodeverkDto korrekt
 */
@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        KodeverkClientConfig::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KodeverkClientTest(
    @Autowired private val kodeverkClient: KodeverkClient,
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
    fun `hentKodeverk kaller korrekt URL og returnerer deserialisert dto`() {
        val kodeverkNavn = "Statsborgerskap"
        mockServer.stubFor(
            get(urlPathEqualTo("/api/v1/kodeverk/$kodeverkNavn/koder/betydninger"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """
                            {
                                "betydninger": {
                                    "NOR": [{
                                        "gyldigFra": "1900-01-01",
                                        "gyldigTil": "9999-12-31",
                                        "beskrivelser": {
                                            "nb": {
                                                "term": "Norsk",
                                                "tekst": "Norsk"
                                            }
                                        }
                                    }]
                                }
                            }
                            """
                        )
                )
        )

        val result = kodeverkClient.hentKodeverk(kodeverkNavn)

        result.betydninger shouldContainKey "NOR"
        result.betydninger["NOR"]!!.first().beskrivelser["nb"]!!.term shouldBe "Norsk"
    }

    @Test
    fun `hentKodeverk sender obligatoriske Nav-headere`() {
        val kodeverkNavn = "Landkoder"
        mockServer.stubFor(
            get(urlPathEqualTo("/api/v1/kodeverk/$kodeverkNavn/koder/betydninger"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"betydninger": {}}""")
                )
        )

        kodeverkClient.hentKodeverk(kodeverkNavn)

        mockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v1/kodeverk/$kodeverkNavn/koder/betydninger"))
                .withHeader("Nav-Consumer-Id", equalTo("srvmelosys"))
        )
    }

    @Test
    fun `hentKodeverk sender korrekte query-parametere`() {
        val kodeverkNavn = "Retningsnumre"
        mockServer.stubFor(
            get(urlPathEqualTo("/api/v1/kodeverk/$kodeverkNavn/koder/betydninger"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"betydninger": {}}""")
                )
        )

        kodeverkClient.hentKodeverk(kodeverkNavn)

        mockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v1/kodeverk/$kodeverkNavn/koder/betydninger"))
                .withQueryParam("ekskluderUgyldige", equalTo("false"))
                .withQueryParam("oppslagsdato", equalTo(java.time.LocalDate.MIN.toString()))
                .withQueryParam("spraak", equalTo("nb"))
        )
    }
}
