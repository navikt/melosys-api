package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.integrasjon.sak.SakConsumerImpl
import no.nav.melosys.integrasjon.sak.SakConsumerProducer
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        SakConsumerImpl::class,
        SakConsumerProducer::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
open class SakConsumerTestBase(
    server: MockRestServiceServer, // TODO: Blir ikke brukt i SakConsumer så lag en ny base klasse eller skriv om
    mockPort: Int
) : ConsumerTestBase<String>(server, mockPort){

    override fun createWireMock(): MappingBuilder {
        return WireMock.get("/api/v1/saker/1")
    }

    override fun getMockData(): String {
        return "{}"
    }
}
