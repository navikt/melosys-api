package no.nav.melosys.integrasjon.doksys.distribuerjournalpost

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class DistribuerJournalpostConsumerConfig : WebClientConfig {

    @Bean
    fun distribuerJournalpostWebClient(
        @Value("\${DistribuerJournalpost_v1.url}") url: String,
        webclientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory
    ): WebClient = webclientBuilder
        .baseUrl(url)
        .defaultHeaders { httpHeaders ->
            httpHeaders.contentType = MediaType.APPLICATION_JSON
            httpHeaders.accept = listOf(MediaType.APPLICATION_JSON)
        }
        .filter(genericAuthFilterFactory.getAzureFilter("dokdistfordeling"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot distribuer journalpost feilet."))
        .build()
}
