package no.nav.melosys.integrasjon.inngangsvilkar

import java.util.Collections
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class InngangsvilkarConfig : WebClientConfig {

    @Bean
    fun inngangsvilkaarWebClient(
        @Value("\${Inngangsvilkaar.url}") url: String,
        webclientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    ): WebClient {

        return webclientBuilder
            .baseUrl(url)
            .defaultHeaders { httpHeaders ->
                httpHeaders.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
                httpHeaders.contentType = MediaType.APPLICATION_JSON
            }
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot inngangsvilkår feilet."))
            .build()
    }
}
