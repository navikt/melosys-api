package no.nav.melosys.integrasjon.melosysskjema

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class MelosysSkjemaWebClientConfig : WebClientConfig {

    @Bean
    fun melosysSkjemaWebClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory,
        @Value("\${MELOSYS_SKJEMA_API_URL}") url: String
    ): WebClient = webClientBuilder
        .baseUrl(url)
        .filter(genericAuthFilterFactory.getAzureFilter("melosys-skjema"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot melosys-skjema feilet."))
        .build()
}
