package no.nav.melosys.integrasjon.skattehendelser

import mu.KotlinLogging
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger { }

private val HENT_TIMEOUT = Duration.ofMinutes(2)

@Configuration
class SkattehendelserWebClientConfig {

    @Bean
    fun skattehendelserWebClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
        genericAuthFilterFactory: GenericAuthFilterFactory,
        @Value("\${MELOSYS_SKATTEHENDELSER_URL}") url: String
    ): WebClient = webClientBuilder
        .baseUrl(url)
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
        .filter(genericAuthFilterFactory.getAzureFilter("melosys-skattehendelser"))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot melosys-skattehendelser feilet."))
        .build()

    companion object {
        private const val MAX_IN_MEMORY_SIZE_BYTES = 64 * 1024 * 1024
    }
}

@Service
class SkattehendelserClient(private val skattehendelserWebClient: WebClient) {

    fun hentSkattepliktige(gjelderÅr: Int, årFilter: ÅrFilter, publisertEtter: LocalDateTime?): SkattepliktigeRespons {
        log.info { "Henter skattepliktige fra melosys-skattehendelser for $gjelderÅr, årFilter=$årFilter, publisertEtter=$publisertEtter" }

        return skattehendelserWebClient.get()
            .uri {
                it.path("/api/admin/skattepliktige")
                    .queryParam("gjelderAar", gjelderÅr)
                    .queryParam("aarFilter", årFilter)
                    .apply { publisertEtter?.let { tidspunkt -> queryParam("publisertEtter", tidspunkt) } }
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(SkattepliktigeRespons::class.java)
            // Jobbtråden er delt med saksbehandlingen; et svar som aldri kommer skal ikke holde den.
            .timeout(HENT_TIMEOUT)
            .block() ?: error("Tomt svar fra melosys-skattehendelser")
    }
}

enum class ÅrFilter {
    FOM_AAR,
    INNTEKTSAAR,
}

data class SkattepliktigeRespons(
    val gjelderAar: Int,
    val antall: Int,
    val skattepliktige: List<Skattepliktig>,
)

data class Skattepliktig(
    val gjelderPeriode: String,
    val identifikator: String,
    val sisteHendelseTid: LocalDateTime,
    val inntektsaar: List<String>,
    val antallPubliseringer: Int,
)
