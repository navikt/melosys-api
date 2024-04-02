package no.nav.melosys.integrasjon.faktureringskomponenten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.getunleash.FakeUnleash
//import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.junit.jupiter.api.*
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
        serviceUnderTestMockServer.resetAll()
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
//        MetricsTestConfig.clearMeterRegistry()
    }

    @Test
    fun `lag en fakturaserie`() {
        val json = ObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(
            lagFakturaserieDto()
        )

        serviceUnderTestMockServer.stubFor(
            post("/fakturaserier")
                .withRequestBody(WireMock.equalToJson(json))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("Hei")
                )
        )
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

}
