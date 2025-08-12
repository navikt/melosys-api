package no.nav.melosys.integrasjon.sak

import io.getunleash.Unleash
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import mu.KotlinLogging
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext

private val log = KotlinLogging.logger { }

@Configuration
class SakConsumerConfig(
    @Value("\${SakAPI_v1.url}") private val url: String
) : WebClientConfig {
    @Bean
    fun sakConsumer(
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        webClientBuilder: WebClient.Builder,
        unleash: Unleash
    ): SakConsumerInterface = try {
        if (unleash.isEnabled(ToggleName.SAK_API_WEBCLIENT)) {
            val webClient = webClientBuilder
                .baseUrl(url)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot Sak API feilet"))
                .build()
            WebClientSakConsumer(webClient)
        } else {
            val webTarget = createWebTarget(url)
            WebTargetSakConsumer(webTarget)
        }
    } catch (e: NoSuchAlgorithmException) {
        log.error("Feilet under oppsett av integrasjon mot Sak API", e)
        throw IntegrasjonException("Feilet under oppsett av integrasjon mot Sak API")
    }

    private fun createWebTarget(url: String): WebTarget =
        ClientBuilder.newBuilder()
            .sslContext(SSLContext.getDefault())
            .build()
            .register(JacksonObjectMapperProvider::class.java)
            .target(url)
}
