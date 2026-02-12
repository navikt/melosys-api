package no.nav.melosys.integrasjon.eessi

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
class EessiClientConfig(
    @Value("\${MelosysEessi.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory
) {
    @Bean
    fun eessiClient(
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        webClientBuilder: WebClient.Builder
    ): EessiClient = EessiClient(
        webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot eessi feilet"))
            .defaultHeaders { headers ->
                headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            }
            .build()
    )

    companion object {
        private const val CLIENT_NAME = "melosys-eessi"
    }
}
