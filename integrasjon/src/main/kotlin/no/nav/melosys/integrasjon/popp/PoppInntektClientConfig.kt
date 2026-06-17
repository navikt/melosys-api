package no.nav.melosys.integrasjon.popp

import mu.KotlinLogging
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.errorFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import org.slf4j.MarkerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }
private val TEAM_LOGS = MarkerFactory.getMarker("TEAM_LOGS")

@Configuration
class PoppInntektClientConfig(
    @Value("\${popp.rest.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory,
) : CallIdAware {

    @Bean
    fun poppInntektClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    ): PoppInntektClient = PoppInntektClient(
        webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
            .filter(headerFilter())
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av pensjonsopptjening fra POPP feilet") { feilmelding, statusCode, errorBody ->
                // Kontrakt: kun 404 PERSON_IKKE_FUNNET tolkes som "ingen PGI" (→ tom liste i klienten).
                // Alle andre feil, inkludert andre 404, er tekniske og kastes videre.
                if (statusCode == HttpStatus.NOT_FOUND && errorBody.contains(PERSON_IKKE_FUNNET_CODE)) {
                    PoppPersonIkkeFunnetException("$feilmelding $statusCode")
                } else {
                    // POPP-feilrespons kan inneholde fnr; rå body logges kun til team-logs, aldri i exception (som ellers eksponeres i HTTP-respons)
                    log.warn(TEAM_LOGS, "$feilmelding $statusCode - $errorBody")
                    TekniskException("$feilmelding $statusCode")
                }
            })
            .build()
    )

    private fun headerFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
            Mono.just(
                ClientRequest.from(request)
                    .header("Nav-Call-Id", callID)
                    .header("Nav-Consumer-Id", CONSUMER_ID)
                    .build()
            )
        }

    companion object {
        private const val CONSUMER_ID = "srvmelosys"
        private const val CLIENT_NAME = "popp"
        private const val PERSON_IKKE_FUNNET_CODE = "PERSON_IKKE_FUNNET"
    }
}
