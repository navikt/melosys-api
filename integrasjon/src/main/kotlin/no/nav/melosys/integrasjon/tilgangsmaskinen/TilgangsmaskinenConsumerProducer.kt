package no.nav.melosys.integrasjon.tilgangsmaskinen

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for Tilgangsmaskinen Consumer og Producer
 */
@Configuration
class TilgangsmaskinenConsumerProducer(
    @Value("\${tilgangsmaskinen.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) : WebClientConfig {

    @Bean
    fun tilgangsmaskineConsumer(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): TilgangsmaskinenConsumer {
        return TilgangsmaskinenConsumerImpl(
            webClientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot Tilgangsmaskinen feilet"))
                .defaultHeaders(this::defaultHeaders)
                .build()
        )
    }

    private fun defaultHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    companion object {
        private const val CLIENT_NAME = "tilgangsmaskinen"
    }
}
