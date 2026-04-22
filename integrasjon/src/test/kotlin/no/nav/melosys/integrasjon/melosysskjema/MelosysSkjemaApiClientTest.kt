package no.nav.melosys.integrasjon.melosysskjema

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
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
              "skjema": {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "status": "SENDT",
                "type": "UTSENDT_ARBEIDSTAKER",
                "fnr": "12345678901",
                "orgnr": "123456789",
                "opprettetDato": "2024-01-15T10:00:00",
                "endretDato": "2024-01-15T10:30:00",
                "metadata": {
                  "metadatatype": "UTSENDT_ARBEIDSTAKER_DEG_SELV",
                  "skjemadel": "ARBEIDSTAKERS_DEL",
                  "arbeidsgiverNavn": "Test Bedrift AS",
                  "juridiskEnhetOrgnr": "987654321"
                },
                "data": {
                  "type": "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL",
                  "utsendingsperiodeOgLand": {
                    "utsendelseLand": "SE",
                    "utsendelsePeriode": {
                      "fraDato": "2024-01-01",
                      "tilDato": "2024-12-31"
                    }
                  }
                }
              },
              "kobletSkjema": null,
              "tidligereInnsendteSkjema": [],
              "referanseId": "MEL-5CA141",
              "innsendtTidspunkt": "2024-01-15T10:30:00",
              "innsenderFnr": "12345678901"
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

        val deserializedResponse = melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId)

        deserializedResponse shouldBe UtsendtArbeidstakerSkjemaM2MDto(
            skjema = UtsendtArbeidstakerSkjemaDto(
                id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                status = SkjemaStatus.SENDT,
                type = SkjemaType.UTSENDT_ARBEIDSTAKER,
                fnr = "12345678901",
                orgnr = "123456789",
                opprettetDato = LocalDateTime.parse("2024-01-15T10:00:00"),
                endretDato = LocalDateTime.parse("2024-01-15T10:30:00"),
                metadata = DegSelvMetadata(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    arbeidsgiverNavn = "Test Bedrift AS",
                    juridiskEnhetOrgnr = "987654321"
                ),
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.SE,
                        utsendelsePeriode = PeriodeDto(
                            fraDato = LocalDate.of(2024, 1, 1),
                            tilDato = LocalDate.of(2024, 12, 31)
                        )
                    )
                )
            ),
            kobletSkjema = null,
            tidligereInnsendteSkjema = emptyList(),
            referanseId = "MEL-5CA141",
            innsendtTidspunkt = LocalDateTime.parse("2024-01-15T10:30:00"),
            innsenderFnr = "12345678901"
        )

        wireMockServer.verify(
            WireMock.getRequestedFor(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .withHeader("Authorization", WireMock.matching("Bearer .+"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
        )
    }

    @Test
    fun `registrerSaksnummer - sender POST med saksnummer`() {
        val skjemaId = UUID.randomUUID()
        val saksnummer = "MEL-1234"

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/saksnummer"))
                .willReturn(WireMock.aResponse().withStatus(204))
        )

        melosysSkjemaApiClient.registrerSaksnummer(skjemaId, saksnummer)

        wireMockServer.verify(
            WireMock.postRequestedFor(WireMock.urlPathEqualTo("/m2m/api/skjema/$skjemaId/saksnummer"))
                .withHeader("Authorization", WireMock.matching("Bearer .+"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(WireMock.matchingJsonPath("$.saksnummer", WireMock.equalTo(saksnummer)))
        )
    }
}
