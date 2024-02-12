package no.nav.melosys.integrasjon.ereg.organisasjon

import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class OrganisasjonRestConsumerConfig(
    @Value("\${ereg.rest.url}") private val url: String
) : WebClientConfig, CallIdAware {

    @Bean
    fun organisasjonRestConsumer(
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
                    .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                    .build()
            )
        }
    }

    override fun lagException(feilmelding: String, statusCode: HttpStatusCode, errorBody: String): Exception {
        if (statusCode == HttpStatus.NOT_FOUND)
            return IkkeFunnetException("$feilmelding $statusCode - $errorBody")
        return super.lagException(feilmelding, statusCode, errorBody)
    }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
    }
}
