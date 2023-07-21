package no.nav.melosys.integrasjon.inntk.inntekt

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
class InntektRestConsumerConfig(
    @Value("\${inntekt.rest.url}") private val url: String
) : WebClientConfig, CallIdAware {

    @Bean
    fun inntektRestConsumerConsumer(
        webClientBuilder: WebClient.Builder,
        systemContextExchangeFilter: GenericAuthFilterFactory,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): InntektRestConsumer {
        return InntektRestConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(systemContextExchangeFilter.getAzureFilter(CLIENT_NAME))
                .filter(headerFilter())
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Henting av inntekt fra inntektskomponenten feilet"))
                .build()
        )
    }

    private fun headerFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
            Mono.just(
                ClientRequest.from(request)
                    .header("Nav-Call-Id", callID)
                    .header("Nav-Consumer-Id", CONSUMER_ID)
                    .build()
            )
        }
    }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
        private const val CLIENT_NAME = "inntekt"
    }
}
