package no.nav.melosys.integrasjon.kodeverk.impl

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.medl.MedlemskapRestConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class KodeverkConsumerProducer(
    @param:Value("\${KodeverkAPI_v1.url}") private val endpointUrl: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) : CallIdAware, WebClientConfig {

    @Bean
    fun kodeverkConsumer(
        webClientBuilder: WebClient.Builder, correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ) = KodeverkConsumerImpl(
        webClientBuilder
            .baseUrl(endpointUrl)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
            .filter(headerFilter())
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot felles-kodeverk feilet."))
            .build()
    )

    private fun headerFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest? ->
            Mono.just(
                ClientRequest.from(request!!)
                    .header("Nav-Call-Id", callID)
                    .header("Nav-Consumer-Id", CONSUMER_ID)
                    .build()
            )
        }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
        private val CLIENT_NAME = "felleskodeverk"
    }
}
