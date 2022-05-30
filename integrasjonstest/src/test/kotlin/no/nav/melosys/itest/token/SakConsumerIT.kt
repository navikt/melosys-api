package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClientClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.integrasjon.sak.SakConsumer
import no.nav.melosys.integrasjon.sak.SakConsumerImpl
import no.nav.melosys.integrasjon.sak.SakConsumerProducer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClientClient::class,
        WebClientAutoConfiguration::class,

        SakConsumerImpl::class,
        SakConsumerProducer::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class SakConsumerIT(
    @Autowired private val sakConsumer: SakConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerTestBase<String>(server, mockPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Basic dGVzdDp0ZXN0")),
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
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Basic dGVzdDp0ZXN0")),
            )
        )
        executeRequest()
    }

    override fun getMockData(): String {
        return "{}"
    }

    override fun executeRequest() {
        sakConsumer.hentSak(1L)
    }
}
