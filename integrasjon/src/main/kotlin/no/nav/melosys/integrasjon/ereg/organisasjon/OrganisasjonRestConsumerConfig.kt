package no.nav.melosys.integrasjon.ereg.organisasjon

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import javax.ws.rs.core.MediaType

@Configuration
class OrganisasjonRestConsumerConfig(
    @Value("\${ereg.rest.url}") private val url: String
) : WebClientConfig, CallIdAware {

    @Bean
    fun inntektRestConsumerConsumer(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): OrganisasjonRestConsumer {
        return OrganisasjonRestConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(headerFilter())
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Henting av organisasjon fra ereg feilet"))
                .build()
        )
    }

    private fun headerFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
            Mono.just(
                ClientRequest.from(request)
                    .header("Nav-Call-Id", callID)
                    .header("Nav-Consumer-Id", CONSUMER_ID)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build()
            )
        }
    }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
    }
}
