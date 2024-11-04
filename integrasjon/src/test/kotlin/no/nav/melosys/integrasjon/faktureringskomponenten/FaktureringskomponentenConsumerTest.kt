package no.nav.melosys.integrasjon.faktureringskomponenten

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.StsMockServer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Import(
    OAuthMockServer::class,
    GenericAuthFilterFactory::class,
    StsWebClientProducer::class,
    SecurityTokenServiceConsumer::class,
    RestSTSService::class,
    StsMockServer::class,

    CorrelationIdOutgoingFilter::class,
    FaktureringskomponentenConsumerProducer::class,
    FakeUnleash::class
)
@WebMvcTest
@AutoConfigureWebClient
@EnableOAuth2Client
@ActiveProfiles("wiremock-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktureringskomponentenConsumerTest(
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
) {

    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
        oAuthMockServer.reset()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun before() {
        serviceUnderTestMockServer.resetAll()
    }

    @Test
    fun `lag en fakturaserie`() {
        val json = """
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
        """.trimIndent()

        serviceUnderTestMockServer.stubFor(
            post("/fakturaserier")
                .withRequestBody(WireMock.equalToJson(json))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"fakturaserieReferanse\": \"456\" }")
                )
        )

        val nyFakturaserieResponseDto = faktureringskomponentenConsumer.lagFakturaserie(lagFakturaserieDto(), "melosys")
        nyFakturaserieResponseDto.fakturaserieReferanse.shouldBe("456")
    }

    @Test
    fun `lag en faktura`() {
        val json = """
            {
                "fodselsnummer":"12345678911",
                "fakturaserieReferanse":null,
                "fullmektig":
                  {
                    "fodselsnummer":"11987654321",
                    "organisasjonsnummer":"123456789"
                  },
                "referanseBruker":"Nasse Nøff",
                "referanseNAV":"NAV Medlemskap og avgift",
                "fakturaGjelderInnbetalingstype":"TRYGDEAVGIFT",
                "intervall":"SINGEL",
                "belop":2000,
                "startDato":"2024-01-01",
                "sluttDato":"2024-12-31",
                "beskrivelse":"Medlemskapsperiode 2024-01-01 - 2024-12-31 endelig beregnet trygdeavgift 2000 - forskuddsvis fakturert trygdeavgift 2000"
            }
        """.trimIndent()

        serviceUnderTestMockServer.stubFor(
            post("/faktura")
                .withRequestBody(WireMock.equalToJson(json))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"fakturaserieReferanse\": \"123\" }")
                )
        )

        val nyFakturaserieResponseDto = faktureringskomponentenConsumer.lagFaktura(lagFakturaDto(), "melosys")
        nyFakturaserieResponseDto.fakturaserieReferanse.shouldBe("123")
    }

    fun get(url: String): MappingBuilder =
        WireMock.get(url)
            .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
            .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))

    fun post(url: String): MappingBuilder =
        WireMock.post(url)
            .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
            .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))


    private fun lagFakturaserieDto(
        fakturaserieReferanse: String? = null,
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV Medlemskap og avgift",
        fakturaGjelder: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        intervall: FaktureringsIntervall = FaktureringsIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123),
                LocalDate.now(),
                LocalDate.now(),
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
        fakturaserieReferanse: String? = null,
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV Medlemskap og avgift",
        fakturaGjelder: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        intervall: FaktureringsIntervall = FaktureringsIntervall.SINGEL,
        belop: BigDecimal = BigDecimal.valueOf(2000),
        startDato: LocalDate = LocalDate.of(LocalDate.now().year, 1, 1),
        sluttDato: LocalDate = LocalDate.of(LocalDate.now().year, 12, 31),
    ): FakturaDto {
        return FakturaDto(
            fodselsnummer,
            fakturaserieReferanse,
            fullmektig,
            referanseBruker,
            referanseNav,
            fakturaGjelder,
            intervall,
            belop,
            startDato,
            sluttDato,
            "Medlemskapsperiode $startDato - $sluttDato endelig beregnet trygdeavgift $belop - forskuddsvis fakturert trygdeavgift $belop"
        )
    }

}
