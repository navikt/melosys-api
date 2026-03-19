package no.nav.melosys.integrasjon.faktureringskomponenten

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        GenericAuthFilterFactory::class,

        CorrelationIdOutgoingFilter::class,
        FaktureringskomponentenClientConfig::class,
        FakeUnleash::class
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktureringskomponentenClientTest(
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val faktureringskomponentenClient: FaktureringskomponentenClient,
) {

    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))
    private val mockServer get() = serviceUnderTestMockServer

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        serviceUnderTestMockServer.resetAll()
        oAuthMockServer.reset()
    }

    @Test
    fun `lag en fakturaserie`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"fakturaserieReferanse\": \"456\" }")
                )
        )

        val nyFakturaserieResponseDto = faktureringskomponentenClient.lagFakturaserie(lagFakturaserieDto(), "melosys")
        nyFakturaserieResponseDto.fakturaserieReferanse.shouldBe("456")

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/fakturaserier"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "fodselsnummer": "12345678911",
                          "fakturaserieReferanse": null,
                          "fullmektig": {
                            "fodselsnummer": "11987654321",
                            "organisasjonsnummer": "123456789"
                          },
                          "referanseBruker": "Nasse Nøff",
                          "referanseNAV": "NAV Medlemskap og avgift",
                          "fakturaGjelderInnbetalingstype": "TRYGDEAVGIFT",
                          "intervall": "KVARTAL",
                          "perioder": [
                            {
                              "enhetsprisPerManed": 123,
                              "startDato": "2024-11-04",
                              "sluttDato": "2024-11-04",
                              "beskrivelse": "Beskrivelse"
                            }
                          ]
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `lag en faktura`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"fakturaserieReferanse\": \"123\" }")
                )
        )

        val nyFakturaserieResponseDto = faktureringskomponentenClient.lagFaktura(lagFakturaDto(), "melosys")
        nyFakturaserieResponseDto.fakturaserieReferanse.shouldBe("123")

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/fakturaer"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "fodselsnummer":"12345678911",
                            "fakturaserieReferanse":"483756934",
                            "fullmektig":
                              {
                                "fodselsnummer":"11987654321",
                                "organisasjonsnummer":"123456789"
                              },
                            "referanseBruker":"Nasse Nøff",
                            "referanseNAV":"NAV Medlemskap og avgift",
                            "fakturaGjelderInnbetalingstype":"TRYGDEAVGIFT",
                            "belop":2000,
                            "startDato":"2024-01-01",
                            "sluttDato":"2024-12-31",
                            "beskrivelse":"Medlemskapsperiode 2024-01-01 - 2024-12-31 endelig beregnet trygdeavgift 2000 - forskuddsvis fakturert trygdeavgift 2000"
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `kanseller fakturaserie med årsavregning`() {
        val referanse = "test-fakturaserie-referanse"

        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"fakturaserieReferanse\": \"5689\" }")
                )
        )

        val nyFakturaserieResponseDto =
            faktureringskomponentenClient.kansellerFakturaserie(referanse, "", listOf("ÅRSAVREGNING-2024-ABC123", "ÅRSAVREGNING-2023-XYZ789"))
        nyFakturaserieResponseDto.fakturaserieReferanse.shouldBe("5689")

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/fakturaserier/$referanse/kanseller"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                          "årsavregningRef": [
                            "ÅRSAVREGNING-2024-ABC123",
                            "ÅRSAVREGNING-2023-XYZ789"
                          ]
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    private fun lagFakturaserieDto(
        fakturaserieReferanse: String? = null,
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV Medlemskap og avgift",
        fakturaGjelder: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        intervall: FaktureringIntervall = FaktureringIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123),
                LocalDate.of(2024, 11, 4),
                LocalDate.of(2024, 11, 4),
                "Beskrivelse"
            )
        ),
    ): FakturaserieDto {
        return FakturaserieDto(
            fodselsnummer,
            fakturaserieReferanse,
            fullmektig,
            referanseBruker,
            referanseNav,
            fakturaGjelder,
            intervall,
            fakturaseriePeriode
        )
    }

    private fun lagFakturaDto(
        fakturaserieReferanse: String = "483756934",
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV Medlemskap og avgift",
        fakturaGjelder: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        belop: BigDecimal = BigDecimal.valueOf(2000),
        startDato: LocalDate = LocalDate.of(2024, 1, 1),
        sluttDato: LocalDate = LocalDate.of(2024, 12, 31),
    ): FakturaDto {
        return FakturaDto(
            fodselsnummer,
            fakturaserieReferanse,
            fullmektig,
            referanseBruker,
            referanseNav,
            fakturaGjelder,
            belop,
            startDato,
            sluttDato,
            "Medlemskapsperiode $startDato - $sluttDato endelig beregnet trygdeavgift $belop - forskuddsvis fakturert trygdeavgift $belop"
        )
    }

}
