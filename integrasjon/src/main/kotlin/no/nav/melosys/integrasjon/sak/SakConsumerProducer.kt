package no.nav.melosys.integrasjon.sak

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SakConsumerProducer(
    @Value("\${SakAPI_v1.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) : WebClientConfig {
    @Bean
    fun sakConsumer(
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        webClientBuilder: WebClient.Builder
    ): SakConsumer {
        return object : SakConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter("sak"))
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot sakconsumer feilet"))
                .defaultHeaders { httpHeaders: HttpHeaders -> defaultHeaders(httpHeaders) }
                .build()) {}
    }

    private fun defaultHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }
}
