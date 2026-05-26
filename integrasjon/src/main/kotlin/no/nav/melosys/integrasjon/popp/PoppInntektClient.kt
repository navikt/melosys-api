package no.nav.melosys.integrasjon.popp

import mu.KotlinLogging
import no.nav.melosys.exception.IkkeFunnetException
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger { }

@Retryable
open class PoppInntektClient(private val webClient: WebClient) {
    open fun hentInntekt(request: PoppHentInntektRequest): PoppHentInntektResponse =
        try {
            webClient.post()
                .uri("/inntekt/hent")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<PoppHentInntektResponse>()
                .block() ?: PoppHentInntektResponse()
        } catch (ex: IkkeFunnetException) {
            log.info { "POPP returnerte 404 for fnr-oppslag — tolker som tom liste (${ex.message})" }
            PoppHentInntektResponse()
        }
}
