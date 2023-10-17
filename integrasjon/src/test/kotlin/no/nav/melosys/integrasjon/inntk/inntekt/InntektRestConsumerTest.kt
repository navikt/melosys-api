package no.nav.melosys.integrasjon.inntk.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonRestConsumerTest
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Year
import java.time.YearMonth
import java.util.*

@Import(
    OAuthMockServer::class,
    CorrelationIdOutgoingFilter::class,

    GenericAuthFilterFactory::class,
    InntektRestConsumerConfig::class,
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektRestConsumerTest(
    @Autowired private val inntektRestConsumer: InntektRestConsumer,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testRestStsClient(): RestStsClient =
            // Må ha det så lenge GenericAuthFilterFactory bruker RestStsClient
            RestStsClient { -> throw IllegalStateException("Should not be called") }
    }

    private val processUUID = UUID.randomUUID()
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
    }

    @BeforeEach
    fun before() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
        serviceUnderTestMockServer.resetAll()
        oAuthMockServer.reset()
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }


    @Test
    fun `hent inntekt liste og sjekk at vi bruker token fra azure`() {
        serviceUnderTestMockServer.stubFor(
            WireMock.post("/inntektskomponenten/rs/api/v1/hentinntektliste")
                .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))
                )
        )

        val inntektListe = inntektRestConsumer.hentInntektListe(
            InntektRequest(
                ainntektsfilter = "MedlemskapA-inntekt",
                formaal = "Medlemskap",
                ident = Aktoer("personID", AktoerType.AKTOER_ID),
                maanedFom = YearMonth.of(2022, 1),
                maanedTom = YearMonth.of(2022, 3),
            )
        )

        inntektListe.arbeidsInntektMaaned
            .shouldNotBeNull()
            .shouldHaveSize(4)
            .map { it.arbeidsInntektInformasjon?.inntektListe }
            .run {
                get(0).shouldNotBeNull().shouldHaveSize(1)
                    .first().run {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull()
                            .tilleggsinformasjonDetaljer.shouldBeInstanceOf<InntektResponse.BonusFraForsvaret>()
                            .aaretUtbetalingenGjelderFor.shouldBe(Year.of(1980))
                    }
                get(1).shouldNotBeNull().shouldHaveSize(1)
                    .first().run {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull()
                            .tilleggsinformasjonDetaljer.shouldBeInstanceOf<InntektResponse.ReiseKostOgLosji>().run {
                                persontype.shouldBe("norskPendler")

                            }
                    }
                get(2).shouldNotBeNull().shouldHaveSize(1)
                    .first().run {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull()
                            .tilleggsinformasjonDetaljer.shouldBeInstanceOf<InntektResponse.Svalbardinntekt>().run {
                                betaltTrygdeavgift.shouldBe(BigDecimal(50000))
                                antallDager.shouldBe(40)

                            }
                    }
                get(3).shouldNotBeNull()
                    .shouldHaveSize(1)
                    .first().tilleggsinformasjon.shouldBeNull()

            }


    }

    fun hentRessurs(fil: String): String = OrganisasjonRestConsumerTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
