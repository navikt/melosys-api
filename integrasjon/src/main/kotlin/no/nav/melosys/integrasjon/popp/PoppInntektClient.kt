package no.nav.melosys.integrasjon.popp

import mu.KotlinLogging
import no.nav.melosys.exception.TekniskException
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger { }

@Retryable(
    retryFor = [TekniskException::class, WebClientRequestException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 500, multiplier = 2.0, maxDelay = 4000),
)
open class PoppInntektClient(private val webClient: WebClient) {
    open fun hentInntekt(request: PoppHentInntektRequest): PoppHentInntektResponse =
        try {
            val respons = webClient.post()
                .uri("/inntekt/hentgrunnlag")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<PoppHentInntektResponse>()
                .block()
                ?: throw TekniskException("POPP returnerte tom respons (200 uten body)")
            respons.copy(inntekter = respons.inntekter ?: emptyList())
        } catch (_: PoppPersonIkkeFunnetException) {
            log.info { "POPP returnerte PERSON_IKKE_FUNNET — tolker som tom liste" }
            PoppHentInntektResponse(inntekter = emptyList())
        }
}
