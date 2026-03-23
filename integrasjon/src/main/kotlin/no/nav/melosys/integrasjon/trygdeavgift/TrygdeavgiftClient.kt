package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningResponse
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.json.JsonMapper


@Component
@Retryable
class TrygdeavgiftClient(
    @Value("\${melosystrygdeavgift.url}") url: String,
    webClientBuilder: WebClient.Builder,
) {
    private val webClient: WebClient = webClientBuilder
        .baseUrl(url)
        .defaultHeaders { httpHeaders: HttpHeaders -> defaultHeaders(httpHeaders) }
        .codecs { configurer ->
            // Egen ObjectMapper uten MelosysModule for kall til melosys-trygdeavgift-beregning.
            // MelosysModule sin KodeSerializer konverterer Kodeverk-enums (f.eks. Avgiftsdekning)
            // til {"kode":"...","term":"..."} objekter, men tjenesten forventer enkle strenger.
            configurer.defaultCodecs().jacksonJsonEncoder(
                JacksonJsonEncoder(JsonMapper.builder().build())
            )
        }
        .build()

    private fun defaultHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }

    fun beregnTrygdeavgift(trygdeavgiftsberegningRequest: TrygdeavgiftsberegningRequest): List<TrygdeavgiftsberegningResponse> =
        webClient.post()
            .uri("/v2/beregn")
            .bodyValue(trygdeavgiftsberegningRequest)
            .retrieve()
            .bodyToMono<List<TrygdeavgiftsberegningResponse>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/beregn")

    fun beregnTrygdeavgiftEosPensjonist(eøsPensjonistTrygdeavgiftsberegningRequest: EøsPensjonistTrygdeavgiftsberegningRequest): List<EøsPensjonistTrygdeavgiftsberegningResponse> =
        webClient.post()
            .uri("/v2/eos-pensjonist/beregn")
            .bodyValue(eøsPensjonistTrygdeavgiftsberegningRequest)
            .retrieve()
            .bodyToMono<List<EøsPensjonistTrygdeavgiftsberegningResponse>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/eos-pensjonist/beregn")
}
