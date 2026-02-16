package no.nav.melosys.integrasjon.sak

import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SakClientConfig(
    @Value("\${SakAPI_v1.url}") private val url: String
) {
    @Bean
    fun sakClient(
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        webClientBuilder: WebClient.Builder
    ): SakClientInterface {
        val webClient = webClientBuilder
            .baseUrl(url)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot Sak API feilet"))
            .build()
        return BasicAuthSakClient(webClient)
    }
}
