package no.nav.melosys.integrasjon.inntk.inntekt

import no.nav.melosys.exception.TekniskException
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Retryable
open class InntektRestConsumer(private val webClient: WebClient) {

    fun hentInntektListe(inntektRequest: InntektRequest) : InntektResponse {
        return webClient.post()
            .uri("/hentinntektliste")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(inntektRequest)
            .retrieve()
            .bodyToMono<InntektResponse>()
            .block() ?: throw TekniskException("InntektResponse er null")
    }
}
