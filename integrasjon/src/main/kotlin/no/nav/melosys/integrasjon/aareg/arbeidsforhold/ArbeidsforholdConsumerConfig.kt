package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import io.getunleash.Unleash
import no.nav.melosys.featuretoggle.ToggleName
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
        stsAuthExchangeFilter: StsAuthExchangeFilter,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        unleash: Unleash
    ): ArbeidsforholdConsumer = if (unleash.isEnabled(ToggleName.MELOSYS_AAREG_AZURE)) {
        consumerWithAzureAuth(webClientBuilder, authFilterFactory, correlationIdOutgoingFilter)
    } else consumerWithStsAuth(webClientBuilder, stsAuthExchangeFilter, correlationIdOutgoingFilter)

    private fun consumerWithAzureAuth(
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

    private fun consumerWithStsAuth(
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

    companion object {
        private const val CLIENT_NAME = "aareg"
        private const val NAV_CONSUMER_ID_NAME = "Nav-Consumer-Id"
        private const val MELOSYS_CONSUMER_ID = "srvmelosys"
    }
}
