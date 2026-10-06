package no.nav.melosys.integrasjon.dokgen

import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.exception.IkkeRetrybarIntegrasjonException
import no.nav.melosys.integrasjon.felles.lagException
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import tools.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class DokgenClientProducer(
    @Value("\${melosysdokgen.v1.url}") private val url: String
) {
    @Bean
    fun dokgenClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter
    ): DokgenClient {
        // Egen JsonMapper uten MelosysModule. MelosysModule serialiserer kodeverk-enums som objekter.
        val dokgenJsonMapper = JsonMapper.builder().build()

        return DokgenClient(
            webClientBuilder
                .baseUrl(url)
                .codecs { configurer ->
                    configurer.defaultCodecs()
                        .jacksonJsonEncoder(JacksonJsonEncoder(dokgenJsonMapper))
                }
                .filter(
                    errorFilter("Kall mot dokumentgenereringstjeneste feilet.") { feilmelding, statusCode, errorBody ->
                        if (statusCode.is4xxClientError) {
                            IkkeRetrybarIntegrasjonException("$feilmelding $statusCode - $errorBody")
                        } else {
                            lagException(feilmelding, statusCode, errorBody)
                        }
                    }
                )
                .filter(correlationIdOutgoingFilter)
                .build()
        )
    }
}
