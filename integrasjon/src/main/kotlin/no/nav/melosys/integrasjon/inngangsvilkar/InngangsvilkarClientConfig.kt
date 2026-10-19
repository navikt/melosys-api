package no.nav.melosys.integrasjon.inngangsvilkar

import java.util.Collections
import no.nav.melosys.integrasjon.felles.errorFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class InngangsvilkarClientConfig {

    @Bean
    fun inngangsvilkaarClient(
        @Value("\${Inngangsvilkaar.url}") url: String,
        webclientBuilder: WebClient.Builder,
    ) = InngangsvilkaarClient(
        webclientBuilder
            .baseUrl(url)
            .defaultHeaders { httpHeaders ->
                httpHeaders.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
                httpHeaders.contentType = MediaType.APPLICATION_JSON
            }
            .filter(errorFilter("Kall mot inngangsvilkår feilet."))
            .build()
    )
}
