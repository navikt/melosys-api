package no.nav.melosys.integrasjon.azuread

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.WebClientConfig
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import reactor.netty.transport.ProxyProvider.TypeSpec
import java.net.URI

@Configuration
class AzureAdConsumerProducer(
    @Value("\${microsoft.graph.rest.url}") private val url: String,
    @Value("\${http.proxy:#{null}}") private val httpProxy: String?,
    private val genericAuthFilterFactory: GenericAuthFilterFactory,
) : CallIdAware, WebClientConfig {

    @Bean
    fun azureAdConsumer(
        webClientBuilder: WebClient.Builder, correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    ): AzureAdConsumer {

        return AzureAdConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
                .filter(errorFilter("Kall mot microsoft graph feilet"))
                .apply {
                    try {
                        val proxyURI = URI(httpProxy!!)
                        val httpClient = HttpClient.create()
                            .proxy { proxy: TypeSpec ->
                                proxy.type(ProxyProvider.Proxy.HTTP)
                                    .host(proxyURI.host)
                                    .port(proxyURI.port)
                            }
                        it.clientConnector(ReactorClientHttpConnector(httpClient))
                    } catch (_: Exception) {
                    }
                }
                .build()
        )
    }

    companion object {
        private const val CLIENT_NAME = "graph"
    }
}


