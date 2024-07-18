package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class ArbeidsforholdRestConsumerConfig(@Value("\${arbeidsforhold.rest.url}") private val url: String) : WebClientConfig {
    @Bean
    fun arbeidsforholdRestConsumer(
        webClientBuilder: WebClient.Builder,
        systemContextExchangeFilter: ArbeidsforholdContextExchangeFilter,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): ArbeidsforholdRestConsumer = ArbeidsforholdRestConsumer(
        webClientBuilder.baseUrl(url).filter(systemContextExchangeFilter).filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av arbeidsforhold fra Aareg feilet")).build()
    )
}
