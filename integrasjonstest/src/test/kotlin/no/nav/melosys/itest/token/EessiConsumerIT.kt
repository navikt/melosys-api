package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.eessi.EessiConsumer
import no.nav.melosys.integrasjon.eessi.EessiConsumerImpl
import no.nav.melosys.integrasjon.eessi.EessiConsumerProducer
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate


@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        EessiConsumerImpl::class,
        EessiConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class EessiConsumerIT(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
    @Autowired private val applicationContext: ApplicationContext
) : ConsumerTestBase<String>(server, mockPort) {

    companion object {
        val customizer = MockServerRestTemplateCustomizer()
    }

    @TestConfiguration
    internal class RestTemplateBuilderProvider {

        @Bean
        fun provideBuilder(): RestTemplateBuilder {
            return RestTemplateBuilder(customizer)
        }
    }

    @BeforeEach
    fun before() {
        val stsRestTemplate = getStsRestTemplate()
        val restServiceServer = customizer.servers
            .filterKeys { restTemplate -> restTemplate != stsRestTemplate }
            .values
            .first()

        restServiceServer!!.expect(MockRestRequestMatchers.requestTo("/buc/123/aksjoner"))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    "[]", MediaType.APPLICATION_JSON
                )
            )
    }

    override fun getSecurityMock(): MockRestServiceServer {
        val stsRestTemplate = getStsRestTemplate()
        return customizer.servers[stsRestTemplate]!!
    }

    private fun getStsRestTemplate() = applicationContext
        .autowireCapableBeanFactory
        .getBean("stsRestTemplate") as RestTemplate

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                    Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-system--"))
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("Kall mot PDL feilet.")
        }
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.get("/not used")
    }

    override fun getMockData(): String {
        return "{}"
    }

    override fun executeRequest() {
        eessiConsumer.hentMuligeAksjoner("123")
    }
}

