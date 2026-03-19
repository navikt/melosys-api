package no.nav.melosys.integrasjon.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonRestClientTest
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.util.*

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        InntektClientConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektClientTest(
    @Autowired private val inntektClient: InntektClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {

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
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(hentRessurs("mock/inntekt/inntektClientResponse.json"))
                )
        )

        val inntektListe = inntektClient.hentInntektListe(
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
            .map { it.arbeidsInntektInformasjon.inntektListe }
            .run {
                get(0).shouldNotBeNull().shouldHaveSize(1)
                    .first().run {
                        beloep.shouldBe(BigDecimal(50000))
                        tilleggsinformasjon.shouldNotBeNull().apply {
                            kategori.shouldBe("bla")
                            tilleggsinformasjonDetaljer.shouldBeNull()
                        }
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

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/inntektskomponenten/rs/api/v1/hentinntektliste"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, WireMock.equalTo("Bearer --azure-token-from-system--"))
        )
    }

    @Test
    fun `skal feile med DecodingException når felter som ikke kan være null er null`() {
        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(hentRessurs("mock/inntekt/inntektClientResponse-med-null.json"))
                )
        )

        shouldThrow<DecodingException> {
            inntektClient.hentInntektListe(
                InntektRequest(
                    ainntektsfilter = "MedlemskapA-inntekt",
                    formaal = "Medlemskap",
                    ident = Aktoer("personID", AktoerType.AKTOER_ID),
                    maanedFom = YearMonth.of(2022, 1),
                    maanedTom = YearMonth.of(2022, 3),
                )
            )
        }.message.shouldContain(
            "JSON decoding error: Instantiation of [simple type, class no.nav.melosys.integrasjon.inntekt.InntektResponse\$Inntekt] " +
                "value failed for JSON property fordel due to missing (therefore NULL) value for creator parameter fordel which is a non-nullable type"
        )
    }

    @Test
    fun `hentInntektListe serialiserer request body korrekt`() {
        serviceUnderTestMockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(hentRessurs("mock/inntekt/inntektClientResponse.json"))
                )
        )

        inntektClient.hentInntektListe(
            InntektRequest(
                ainntektsfilter = "MedlemskapA-inntekt",
                formaal = "Medlemskap",
                ident = Aktoer("12345678901", AktoerType.NATURLIG_IDENT),
                maanedFom = YearMonth.of(2023, 1),
                maanedTom = YearMonth.of(2023, 3),
            )
        )

        serviceUnderTestMockServer.verify(
            postRequestedFor(urlEqualTo("/inntektskomponenten/rs/api/v1/hentinntektliste"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "ainntektsfilter": "MedlemskapA-inntekt",
                            "filterversjon": null,
                            "formaal": "Medlemskap",
                            "ident": {
                                "identifikator": "12345678901",
                                "aktoerType": "NATURLIG_IDENT"
                            },
                            "maanedFom": "2023-01",
                            "maanedTom": "2023-03"
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    fun hentRessurs(fil: String): String = OrganisasjonRestClientTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")
}
