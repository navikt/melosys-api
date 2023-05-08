package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftBeregningsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
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

    fun beregnTrygdeavgift(trygdeavgiftBeregningsgrunnlagDto: TrygdeavgiftBeregningsgrunnlagDto): List<TrygdeavgiftsperiodeDto> =
        webClient.post()
            .uri("/v2/beregn")
            .bodyValue(trygdeavgiftBeregningsgrunnlagDto)
            .retrieve()
            .bodyToMono<List<TrygdeavgiftsperiodeDto>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/beregn")
}
