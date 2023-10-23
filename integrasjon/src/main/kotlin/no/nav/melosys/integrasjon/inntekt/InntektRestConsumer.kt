package no.nav.melosys.integrasjon.inntekt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
        println("---------------################-----------------")
        println(inntektRequest.toJsonNode.toPrettyString())
        return webClient.post()
            .uri("/hentinntektliste")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(inntektRequest)
            .retrieve()
            .bodyToMono<InntektResponse>()
            .block() ?: throw TekniskException("InntektResponse er null")
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }

}
