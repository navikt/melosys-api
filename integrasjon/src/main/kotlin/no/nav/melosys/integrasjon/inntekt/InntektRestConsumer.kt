package no.nav.melosys.integrasjon.inntekt

import no.nav.melosys.exception.TekniskException
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Retryable
open class InntektRestConsumer(private val webClient: WebClient) {
    // Metoder må være open for at retry skal funke og at webClient ikke skal bli null
    // https://github.com/spring-projects/spring-framework/issues/26729
    open fun hentInntektListe(inntektRequest: InntektRequest) : InntektResponse {
        return webClient.post()
            .uri("/hentinntektliste")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(inntektRequest)
            .retrieve()
            .bodyToMono<InntektResponse>()
            .block() ?: throw TekniskException("InntektResponse er null")
    }
}
