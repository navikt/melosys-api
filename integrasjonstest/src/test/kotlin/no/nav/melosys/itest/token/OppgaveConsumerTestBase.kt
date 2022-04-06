package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter
import no.nav.melosys.integrasjon.felles.UserContextExchangeFilter
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerImpl
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerProducer
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

        OppgaveConsumerImpl::class,
        OppgaveConsumerProducer::class,
        UserContextExchangeFilter::class,
        SystemContextExchangeFilter::class,
        GenericContextExchangeFilter::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
open class OppgaveConsumerTestBase(
    server: MockRestServiceServer,
    mockPort: Int,
    private val oppgaveConsumer: OppgaveConsumer
) : ConsumerTestBase<String>(server, mockPort) {
    override fun createWireMock(): MappingBuilder {
        return WireMock.get("/api/v1/oppgaver/1")
    }

    override fun getMockData(): String {
        return "{}"
    }

    override fun executeRequest() {
        oppgaveConsumer.hentOppgave("1")
    }
}
