package no.nav.melosys.integrasjon.medl

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.felles.RestConsumer
import no.nav.melosys.integrasjon.felles.WebClientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class MedlemskapRestConsumerProducer(@Value("\${medlemskap.rest.url}") private val url: String) : RestConsumer,
    WebClientConfig {
    @Bean
    @Primary
    fun medlemskapRestConsumer(
        webClientBuilder: WebClient.Builder,
        genericContextExchangeFilter: GenericContextExchangeFilter
    ) = MedlemskapRestConsumer(
        webClientBuilder
            .baseUrl(url)
            .filter(genericContextExchangeFilter)
            .filter(headerFilter())
            .filter(errorFilter("Kall mot Medl feilet."))
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
    }
}
