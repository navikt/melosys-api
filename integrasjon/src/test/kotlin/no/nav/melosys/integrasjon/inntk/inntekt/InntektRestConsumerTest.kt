package no.nav.melosys.integrasjon.inntk.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.integrasjon.OAuthMockServer
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
            WireMock.post("/inntektskomponenten/rs/api/v1")
                .withHeader("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(inntektJsonResponse)
                )
        )

        val inntektListe = inntektRestConsumer.hentInntektListe(
            InntektRequest(
                ainntektsfilter = "MedlemskapA-inntekt",
                formaal = "Medlemskap",
                ident = Aktoer("personID", AktoerType.AKTOER_ID),
                maanedFom = YearMonth.of(2022, 1),
                maanedTom = YearMonth.of(2022, 2),
            )
        )

        inntektListe.arbeidsInntektMaaned
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .map { it.arbeidsInntektInformasjon?.inntektListe }
            .apply {
                first().shouldNotBeNull().shouldHaveSize(1)
                    .first().apply {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull()
                            .tilleggsinformasjonDetaljer.shouldBeInstanceOf<InntektResponse.BonusFraForsvaret>()
                            .aaretUtbetalingenGjelderFor.shouldBe(Year.of(1980))
                    }
                last().shouldNotBeNull().shouldHaveSize(1)
                    .first().apply {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull()
                            .tilleggsinformasjonDetaljer.
                            shouldBeInstanceOf<InntektResponse.Svalbardinntekt>().apply {
                            betaltTrygdeavgift.shouldBe(BigDecimal(50000))
                            antallDager.shouldBe(40)

                        }
                    }
            }
    }

    companion object {
        val inntektJsonResponse = """
            {
                "arbeidsInntektMaaned": [
                    {
                        "aarMaaned": "2022-02",
                        "arbeidsInntektInformasjon": {
                            "inntektListe": [
                                {
                                    "inntektType": "LOENNSINNTEKT",
                                    "beloep": 50000,
                                    "fordel": "kontantytelse",
                                    "inntektskilde": "A-ordningen",
                                    "inntektsperiodetype": "Maaned",
                                    "inntektsstatus": "LoependeInnrapportert",
                                    "leveringstidspunkt": "2023-07",
                                    "utbetaltIMaaned": "2022-02",
                                    "opplysningspliktig": {
                                        "identifikator": "928497704",
                                        "aktoerType": "ORGANISASJON"
                                    },
                                    "virksomhet": {
                                        "identifikator": "907670201",
                                        "aktoerType": "ORGANISASJON"
                                    },
                                    "inntektsmottaker": {
                                        "identifikator": "2395801903843",
                                        "aktoerType": "AKTOER_ID"
                                    },
                                    "inngaarIGrunnlagForTrekk": true,
                                    "utloeserArbeidsgiveravgift": true,
                                    "informasjonsstatus": "InngaarAlltid",
                                    "beskrivelse": "fastloenn",
                                    "tilleggsinformasjon": {
                                        "kategori": "bla",
                                        "tilleggsinformasjonDetaljer": {
                                            "detaljerType": "BONUSFRAFORSVARET",
                                            "aaretUtbetalingenGjelderFor": 1980
                                        }
                                    }
                                }
                            ]
                        }
                    },
                    {
                        "aarMaaned": "2022-01",
                        "arbeidsInntektInformasjon": {
                            "inntektListe": [
                                {
                                    "inntektType": "LOENNSINNTEKT",
                                    "beloep": 50000,
                                    "fordel": "kontantytelse",
                                    "inntektskilde": "A-ordningen",
                                    "inntektsperiodetype": "Maaned",
                                    "inntektsstatus": "LoependeInnrapportert",
                                    "leveringstidspunkt": "2023-07",
                                    "utbetaltIMaaned": "2022-01",
                                    "opplysningspliktig": {
                                        "identifikator": "928497704",
                                        "aktoerType": "ORGANISASJON"
                                    },
                                    "virksomhet": {
                                        "identifikator": "907670201",
                                        "aktoerType": "ORGANISASJON"
                                    },
                                    "inntektsmottaker": {
                                        "identifikator": "2395801903843",
                                        "aktoerType": "AKTOER_ID"
                                    },
                                    "inngaarIGrunnlagForTrekk": true,
                                    "utloeserArbeidsgiveravgift": true,
                                    "informasjonsstatus": "InngaarAlltid",
                                    "beskrivelse": "fastloenn",
                                    "tilleggsinformasjon": {
                                        "kategori": "bla",
                                        "tilleggsinformasjonDetaljer": {
                                            "detaljerType": "SVALBARDINNTEKT",
                                            "antallDager": 40,
                                            "betaltTrygdeavgift": "50000"
                                        }
                                    }
                                }
                            ]
                        }
                    }
                ],
                "ident": {
                    "identifikator": "2395801903843",
                    "aktoerType": "AKTOER_ID"
                }
            }
        """.trimIndent()
    }
}

