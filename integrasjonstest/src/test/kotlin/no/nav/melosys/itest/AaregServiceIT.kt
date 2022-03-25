package no.nav.melosys.itest

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
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
    ],
    properties = ["spring.profiles.active:local-mock"] // TODO: lag egen profil eller få satt arbeidsforhold.rest.url til port som webmock skal bruke
)
internal class AaregServiceIT(
    @Autowired
    private val server: MockRestServiceServer,
    @Autowired
    private val aaregService: AaregService
) {

    @BeforeEach
    fun setup() {
        val environment = Mockito.spy(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
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

          // TODO: bruk MockWebServer siden dette ikke bruker RestTemplateBuilder
//        server.expect(requestTo("?regelverk=A_ORDNINGEN&ansettelsesperiodeFom=2022-03-24&ansettelsesperiodeTom=2022-03-24"))
//            .andRespond(withSuccess("{  }", MediaType.APPLICATION_JSON))

        aaregService.finnArbeidsforholdPrArbeidstaker("121", LocalDate.now(), LocalDate.now())
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

//        server.expect(requestTo("dummy?regelverk=A_ORDNINGEN&ansettelsesperiodeFom=2022-03-24&ansettelsesperiodeTom=2022-03-24"))
//            .andRespond(withSuccess("{  }", MediaType.APPLICATION_JSON))

        aaregService.finnArbeidsforholdPrArbeidstaker("121", LocalDate.now(), LocalDate.now())
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
