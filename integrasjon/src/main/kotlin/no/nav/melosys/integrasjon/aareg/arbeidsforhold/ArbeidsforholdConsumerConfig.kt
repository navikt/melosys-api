package no.nav.melosys.integrasjon.aareg.arbeidsforhold

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
        stsAuthExchangeFilter: StsAuthExchangeFilter,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): ArbeidsforholdConsumer = ArbeidsforholdConsumer(
        webClientBuilder.baseUrl(url)
            .filter(stsAuthExchangeFilter)
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av arbeidsforhold fra Aareg feilet"))
            .build()
    )
}
