package no.nav.melosys.integrasjon.joark.journalpostapi

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class JournalpostapiConsumerConfig : WebClientConfig {

    @Bean
    fun journalpostapiWebClient(
        webclientBuilder: WebClient.Builder,
        @Value("\${JournalpostApi_v1.url}") url: String,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory
    ): WebClient = webclientBuilder
        .baseUrl(url)
        .filter(genericAuthFilterFactory.getAzureFilter("dokarkiv"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot journalpostapi feilet."))
        .defaultHeaders { httpHeaders ->
            httpHeaders.accept = listOf(MediaType.APPLICATION_JSON)
            httpHeaders.contentType = MediaType.APPLICATION_JSON
        }
        .build()

}
