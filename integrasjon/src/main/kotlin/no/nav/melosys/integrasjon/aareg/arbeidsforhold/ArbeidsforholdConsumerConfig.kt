package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class ArbeidsforholdConsumerConfig(@Value("\${arbeidsforhold.rest.url}") private val url: String) : WebClientConfig {
    @Bean
    fun arbeidsforholdConsumer(
        webClientBuilder: WebClient.Builder,
        authFilterFactory: GenericAuthFilterFactory,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ) = ArbeidsforholdConsumer(
        webClientBuilder.baseUrl(url)
            .defaultHeader(NAV_CONSUMER_ID_NAME, MELOSYS_CONSUMER_ID)
            .filter(authFilterFactory.getAzureFilter(CLIENT_NAME))
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av arbeidsforhold fra Aareg feilet"))
            .build()
    )

    companion object {
        private const val CLIENT_NAME = "aareg"
        private const val NAV_CONSUMER_ID_NAME = "Nav-Consumer-Id"
        private const val MELOSYS_CONSUMER_ID = "srvmelosys"
    }
}
