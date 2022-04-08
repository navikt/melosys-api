package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter
import no.nav.melosys.integrasjon.felles.UserContextExchangeFilter
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.integrasjon.joark.saf.SafConsumerImpl
import no.nav.melosys.integrasjon.joark.saf.SafConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        UserContextExchangeFilter::class,
        SystemContextExchangeFilter::class,
        SafConsumerImpl::class,
        SafConsumerProducer::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
open class SafConsumerTestBase(
    server: MockRestServiceServer,
    mockPort: Int,
    private val safConsumer: SafConsumer
) : ConsumerTestBase<ByteArray>(server, mockPort){

    override fun createWireMock(): MappingBuilder {
        return WireMock.get(UrlPattern.ANY)
    }

    override fun getMockData(): ByteArray {
        return ByteArray(0)
    }

    override fun executeRequest() {
        safConsumer.hentDokument("1", "1")
    }
}
