package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningV1Dto
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningV2Dto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.Trygdeavgiftsperiode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono


@Component
@Retryable
class TrygdeavgiftConsumer(@Value("\${melosystrygdeavgift.url}") url: String?) {
    private val webClient: WebClient

    init {
        webClient = WebClient.builder()
            .baseUrl(url!!)
            .defaultHeaders { httpHeaders: HttpHeaders -> defaultHeaders(httpHeaders) }
            .build()
    }

    private fun defaultHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    fun beregnTrygdeavgift(melosysTrygdeavgfitBeregningV1Dto: MelosysTrygdeavgfitBeregningV1Dto?): List<TrygdeavgiftDto> {
        return webClient.post()
            .uri("/v1/beregn-trygdeavgift")
            .bodyValue(melosysTrygdeavgfitBeregningV1Dto!!)
            .retrieve()
            .bodyToMono<List<TrygdeavgiftDto>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v1/beregn-trygdeavgift")
    }

    fun beregnTrygdeavgift(melosysTrygdeavgfitBeregningV2Dto: MelosysTrygdeavgfitBeregningV2Dto?): List<Trygdeavgiftsperiode> {
        return webClient.post()
            .uri("/v2/beregn")
            .bodyValue(melosysTrygdeavgfitBeregningV2Dto!!)
            .retrieve()
            .bodyToMono<List<Trygdeavgiftsperiode>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/beregn")
    }
}
