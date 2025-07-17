package no.nav.melosys.integrasjon.sak

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        SakConsumerProducer::class,
        OAuthMockServer::class,
        GenericAuthFilterFactory::class,
    ]
)
@AutoConfigureWebClient
class SakConsumerTokenTest(
    @Autowired private val sakConsumer: SakConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, SakDto>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --azure-token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeRequest()
    }

    @Test
    fun correlationIdLeggesPåRequest() {
        verifyHeaders(
            mapOf(
                Pair("X-Correlation-ID", WireMock.matching(UUID_REGEX)),
            )
        )
        executeRequest()
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.post(UrlPattern.ANY)
    }

    override fun getMockData(): String {
        return "{}"
    }

    override fun executeRequest() =
        sakConsumer.opprettSak(SakDto())
}
