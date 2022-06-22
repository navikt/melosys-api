package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.integrasjon.sak.SakConsumer
import no.nav.melosys.integrasjon.sak.SakConsumerImpl
import no.nav.melosys.integrasjon.sak.SakConsumerProducer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestTokenServiceClient::class,
        MockRestServerProvider::class,

        SakConsumerImpl::class,
        SakConsumerProducer::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class SakConsumerIT(
    @Autowired private val sakConsumer: SakConsumer,
    @Autowired mockRestServerProvider: MockRestServerProvider,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerWireMockTestBase<String>(mockRestServerProvider, mockPort) {

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
