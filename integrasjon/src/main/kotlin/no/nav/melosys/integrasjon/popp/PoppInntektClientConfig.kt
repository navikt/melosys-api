package no.nav.melosys.integrasjon.popp

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.lagException
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class PoppInntektClientConfig(
    @Value("\${popp.rest.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory,
) : CallIdAware {

    @Bean
    fun poppInntektClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    ): PoppInntektClient = PoppInntektClient(
        webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
            .filter(headerFilter())
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av pensjonsopptjening fra POPP feilet") { feilmelding, statusCode, errorBody ->
                if (statusCode == HttpStatus.NOT_FOUND && errorBody.contains(PERSON_IKKE_FUNNET_CODE))
                    PoppPersonIkkeFunnetException("$feilmelding $statusCode - $errorBody")
                else
                    lagException(feilmelding, statusCode, errorBody)
            })
            .build()
    )

    private fun headerFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
            Mono.just(
                ClientRequest.from(request)
                    .header("Nav-Call-Id", callID)
                    .header("Nav-Consumer-Id", CONSUMER_ID)
                    .build()
            )
        }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
        private const val CLIENT_NAME = "popp"
        private const val PERSON_IKKE_FUNNET_CODE = "PERSON_IKKE_FUNNET"
    }
}
