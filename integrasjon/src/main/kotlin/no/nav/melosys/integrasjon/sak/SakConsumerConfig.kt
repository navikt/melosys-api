package no.nav.melosys.integrasjon.sak

import jakarta.ws.rs.client.ClientBuilder
import mu.KotlinLogging
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext

private val log = KotlinLogging.logger { }

@Configuration
class SakConsumerConfig(@Value("\${SakAPI_v1.url}") private val endpointUrl: String) {
    @Bean
    fun sakConsumer(): SakConsumer {
        return try {
            val sslContext = SSLContext.getDefault()
            val client = ClientBuilder.newBuilder().sslContext(sslContext).build()
            val target = client.register(JacksonObjectMapperProvider::class.java).target(endpointUrl)
            SakConsumer(target)
        } catch (e: NoSuchAlgorithmException) {
            log.error("Feilet under oppsett av integrasjon mot Sak API", e)
            throw IntegrasjonException("Feilet under oppsett av integrasjon mot Sak API")
        }
    }
}
