package no.nav.melosys.integrasjon.aad;

import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.URI

object WebClientProxyConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    fun clientHttpConnector(proxyUri: String): ReactorClientHttpConnector {
        val uri = URI(proxyUri)
        log.info("URI: Host: '{}'. Port: {}.", uri.host, uri.port)

        val httpClient = HttpClient
            .create()
            .proxy { p -> proxySpec(p, uri.host, uri.port) }

        return ReactorClientHttpConnector(httpClient)
    }

    private fun proxySpec(proxy: ProxyProvider.TypeSpec, host: String, port: Int): ProxyProvider.Builder {
        return proxy
            .type(ProxyProvider.Proxy.HTTP)
            .host(host)
            .port(port)
    }
}
