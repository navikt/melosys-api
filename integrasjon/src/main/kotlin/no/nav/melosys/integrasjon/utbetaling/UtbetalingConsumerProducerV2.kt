package no.nav.melosys.integrasjon.utbetaling

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class UtbetalingConsumerProducerV2(
    @Value("\${utbetaling_rest.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) : CallIdAware, WebClientConfig {
    @Bean
    fun utbetalingConsumerV2(
        webClientBuilder: WebClient.Builder, correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ) = UtbetaldataRestConsumer(
        webClientBuilder
            .baseUrl(url)
            .filter(headerFilter())
            .filter(genericAuthFilterFactory.getFilter(CLIENT_NAME))
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot Utbetalinger feilet."))
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
        private const val CLIENT_NAME = "sokos-utbetaldata"
    }
}
