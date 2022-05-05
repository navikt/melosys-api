package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.medl.MedlemskapRestConsumer
import no.nav.melosys.integrasjon.medl.MedlemskapRestConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer
import java.time.LocalDate

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        MedlemskapRestConsumer::class,
        MedlemskapRestConsumerProducer::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
abstract class MedlemskapConsumerTestBase(
    server: MockRestServiceServer,
    mockPort: Int,
    private val medlemskapRestConsumer: MedlemskapRestConsumer
) : ConsumerTestBase<String>(server, mockPort) {

    override fun createWireMock(): MappingBuilder {
        return WireMock.get(UrlPattern.ANY)
    }

    override fun getMockData(): String {
        return "[]"
    }

    override fun executeRequest() {
        val fom = LocalDate.now().minusDays(2)
        val tom = LocalDate.now().plusDays(2)
        val fnr = "12345678990"
        medlemskapRestConsumer.hentPeriodeListe(fnr, fom, tom)
    }
}

