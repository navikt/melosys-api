package no.nav.melosys.integrasjon.melosysskjema

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
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
        MelosysSkjemaApiWebClientConfig::class,
        MelosysSkjemaApiClient::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MelosysSkjemaApiClientTest(
    @Autowired private val melosysSkjemaApiClient: MelosysSkjemaApiClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        wireMockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        oAuthMockServer.reset()
        wireMockServer.resetAll()
    }

    @Test
    fun `hentUtsendtArbeidstakerSkjema - mottar skjemadata - blir mappet`() {
        val skjemaId = UUID.randomUUID()
        val responseJson = """
            {
              "skjemaer": [
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "status": "SENDT",
                  "type": "UTSENDT_ARBEIDSTAKER",
                  "fnr": "12345678901",
                  "orgnr": "123456789",
                  "metadata": {
                    "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
                    "skjemadel": "ARBEIDSTAKERS_DEL",
                    "arbeidsgiverNavn": "Test Bedrift AS",
                    "juridiskEnhetOrgnr": "987654321"
                  },
                  "data": {
                    "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL",
                    "utenlandsoppdraget": {
                      "utsendelsesLand": "SE",
                      "utsendelsePeriode": {
                        "fraDato": "2024-01-01",
                        "tilDato": "2024-12-31"
                      }
                    }
                  }
                }
              ],
              "referanseId": "MEL-5CA141"
            }
        """.trimIndent()

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)
                )
        )

        val resultat = melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId)

        resultat.shouldNotBeNull()
        resultat.referanseId shouldBe "MEL-5CA141"
        resultat.skjemaer shouldHaveSize 1

        val skjema = resultat.skjemaer.first()
        skjema.id shouldBe UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        skjema.status shouldBe SkjemaStatus.SENDT
        skjema.type shouldBe SkjemaType.UTSENDT_ARBEIDSTAKER
        skjema.fnr shouldBe "12345678901"
        skjema.orgnr shouldBe "123456789"

        skjema.metadata.shouldBeInstanceOf<DegSelvMetadata>()
        val metadata = skjema.metadata as DegSelvMetadata
        metadata.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
        metadata.arbeidsgiverNavn shouldBe "Test Bedrift AS"
        metadata.juridiskEnhetOrgnr shouldBe "987654321"

        skjema.data.shouldBeInstanceOf<UtsendtArbeidstakerArbeidstakersSkjemaDataDto>()
        val data = skjema.data as UtsendtArbeidstakerArbeidstakersSkjemaDataDto
        data.utenlandsoppdraget.shouldNotBeNull()
        data.utenlandsoppdraget!!.utsendelsesLand shouldBe LandKode.SE
        data.utenlandsoppdraget!!.utsendelsePeriode.fraDato shouldBe LocalDate.of(2024, 1, 1)
        data.utenlandsoppdraget!!.utsendelsePeriode.tilDato shouldBe LocalDate.of(2024, 12, 31)

        wireMockServer.verify(
            WireMock.getRequestedFor(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .withHeader("Authorization", WireMock.matching("Bearer .+"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
        )
    }
}
