package no.nav.melosys.integrasjon.faktureringskomponenten

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FaktureringskomponentenClientConfig(
    @Value("\${faktureringskomponenten.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) {
    @Bean
    fun faktureringskomponentenConsumer(
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter?,
        webClientBuilder: WebClient.Builder
    ): FaktureringskomponentenClient {
        return object : FaktureringskomponentenClient(webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter("faktureringskomponenten"))
            .filter(correlationIdOutgoingFilter!!)
            .filter(errorFilter("Kall mot Faktureringskomponenten feilet"))
            .defaultHeaders { httpHeaders: HttpHeaders -> defaultHeaders(httpHeaders) }
            .build()) {}
    }

    private fun defaultHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }
}
