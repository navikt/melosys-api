package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.aareg.AaregService
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdContextExchangeFilter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumerConfig
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.LocalDate
import java.util.*

@RestClientTest(
    value = [
        AaregService::class,
        ArbeidsforholdRestConsumerConfig::class,
        ArbeidsforholdContextExchangeFilter::class,
        StsRestTemplateProducer::class,
        RestStsClient::class,
        MockWebServerWrapper::class
    ],
    properties = ["spring.profiles.active:itest-aareg"] // TODO: lag egen profil eller få satt arbeidsforhold.rest.url til port som webmock skal bruke
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaregServiceIT(
    @Autowired
    private val server: MockRestServiceServer,
    @Autowired
    private val aaregService: AaregService,
//    @Autowired
//    private val mockWebServerWrapper: MockWebServerWrapper
) {

    companion object {
        var wireMockServer: WireMockServer =
            WireMockServer(WireMockConfiguration.wireMockConfig().port(8180)).apply { start() }
    }

    @BeforeEach
    fun setup() {
        wireMockServer.start()
        val environment = Mockito.spy(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
        setupMockRequests()
    }

    private fun setupMockRequests() {
//        mockWebServerWrapper.dispatcher { request ->
//            println(request.body.toString())
//            return@dispatcher MockResponse().setBody(
//                """{}"""
//            ).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//        }
    }

    @Test
    fun testRequestFromFront() {

        ThreadLocalAccessInfo.preHandle("request")

        SpringSubjectHandler.set(TestSubjectHandler())
        server.expect(requestTo("/?grant_type=client_credentials&scope=openid"))
            .andRespond(
                withSuccess(
                    "{ \"access_token\": \"Bearer --token--\", \"expires_in\": \"123\" }",
                    MediaType.APPLICATION_JSON
                )
            )

        wireMockServer.stubFor(
            WireMock.get("/?regelverk=A_ORDNINGEN&ansettelsesperiodeFom=2022-03-26&ansettelsesperiodeTom=2022-03-27")
                .withHeader("Nav-Personident", WireMock.equalTo("121"))
                .withHeader("Authorization", WireMock.equalTo("Bearer Bearer --token--"))
                .withQueryParam("regelverk", WireMock.equalTo("A_ORDNINGEN"))
                .withQueryParam("ansettelsesperiodeFom", WireMock.equalTo("2022-03-26"))
                .withQueryParam("ansettelsesperiodeTom", WireMock.equalTo("2022-03-27")).willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )

        val start = LocalDate.of(2022, 3, 26)
        val stop = LocalDate.of(2022, 3, 27)
        aaregService.finnArbeidsforholdPrArbeidstaker("121", start, stop)


        ThreadLocalAccessInfo.afterCompletion("request")
    }

    @Test
    fun testRequestFromProsess() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        SpringSubjectHandler.set(TestSubjectHandler())
        server.expect(requestTo("/?grant_type=client_credentials&scope=openid"))
            .andRespond(
                withSuccess(
                    "{ \"access_token\": \"Bearer --token--\", \"expires_in\": \"123\" }",
                    MediaType.APPLICATION_JSON
                )
            )

        wireMockServer.stubFor(
            WireMock.get("/?regelverk=A_ORDNINGEN&ansettelsesperiodeFom=2022-03-26&ansettelsesperiodeTom=2022-03-27")
                .withHeader("Nav-Personident", WireMock.equalTo("121"))
                .withHeader("Authorization", WireMock.equalTo("Bearer Bearer --token--"))
                .withQueryParam("regelverk", WireMock.equalTo("A_ORDNINGEN"))
                .withQueryParam("ansettelsesperiodeFom", WireMock.equalTo("2022-03-26"))
                .withQueryParam("ansettelsesperiodeTom", WireMock.equalTo("2022-03-27")).willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                )
        )

        val start = LocalDate.of(2022, 3, 26)
        val stop = LocalDate.of(2022, 3, 27)
        aaregService.finnArbeidsforholdPrArbeidstaker("121", start, stop)
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }


    @TestConfiguration
    class TestConfig {
        @Bean
        fun kodeOppslag(): KodeOppslag {
            return KodeOppslagImpl();
        }
    }

    class KodeOppslagImpl : KodeOppslag {
        override fun getTermFraKodeverk(kodeverk: FellesKodeverk?, kode: String?): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }

        override fun getTermFraKodeverk(kodeverk: FellesKodeverk?, kode: String?, dato: LocalDate?): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }

        override fun getTermFraKodeverk(
            kodeverk: FellesKodeverk?,
            kode: String?,
            dato: LocalDate?,
            kodeperioder: MutableList<Kode>?
        ): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }
    }

    class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String {
            return "--token11"
        }

        override fun getUserID(): String {
            return "Z990007"
        }
    }
}
