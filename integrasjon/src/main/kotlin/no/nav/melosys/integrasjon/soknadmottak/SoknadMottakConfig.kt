package no.nav.melosys.integrasjon.soknadmottak

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SoknadMottakConfig : WebClientConfig {

    @Bean
    fun soknadMottakWebClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory,
        @Value("\${MelosysSoknadMottak.url}") url: String
    ): WebClient = webClientBuilder
        .baseUrl(url)
        .filter(genericAuthFilterFactory.getAzureFilter("melosys-soknad-mottak"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot søknad mottak feilet."))
        .build()
}
