package no.nav.melosys.integrasjon.melosysskjema

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class MelosysSkjemaApiWebClientConfig {

    companion object {
        private const val MAX_IN_MEMORY_SIZE_BYTES = 16 * 1024 * 1024

        /** Så et hengende skjema-api ikke blokkerer tråder (kallene går bl.a. fra event-listener). */
        private val RESPONSE_TIMEOUT = Duration.ofSeconds(10)
    }

    @Bean
    fun melosysSkjemaApiWebClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory,
        @Value("\${MELOSYS_SKJEMA_API_URL}") url: String
    ): WebClient = webClientBuilder
        .baseUrl(url)
        .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(RESPONSE_TIMEOUT)))
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
        .filter(genericAuthFilterFactory.getAzureFilter("melosys-skjema"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot melosys-skjema feilet."))
        .build()
}
