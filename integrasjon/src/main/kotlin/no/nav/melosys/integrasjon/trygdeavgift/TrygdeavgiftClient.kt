package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
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
            .headers { leggTilBrukerId(it) }
            .bodyValue(trygdeavgiftsberegningRequest)
            .retrieve()
            .bodyToMono<List<TrygdeavgiftsberegningResponse>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/beregn")

    fun beregnTrygdeavgiftEosPensjonist(eøsPensjonistTrygdeavgiftsberegningRequest: EøsPensjonistTrygdeavgiftsberegningRequest): List<EøsPensjonistTrygdeavgiftsberegningResponse> =
        webClient.post()
            .uri("/v2/eos-pensjonist/beregn")
            .headers { leggTilBrukerId(it) }
            .bodyValue(eøsPensjonistTrygdeavgiftsberegningRequest)
            .retrieve()
            .bodyToMono<List<EøsPensjonistTrygdeavgiftsberegningResponse>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/eos-pensjonist/beregn")

    // Hentes per kall, før WebClient bytter tråd. Sagaer bruker lagret saksbehandler.
    private fun leggTilBrukerId(headers: HttpHeaders) {
        val brukerId = SubjectHandler.getInstance().userID
            ?: ThreadLocalAccessInfo.getSaksbehandler()
        if (!brukerId.isNullOrBlank()) {
            headers.set("Nav-User-Id", brukerId)
        }
    }

    @Cacheable("minstebeloep")
    fun hentMinstebeløp(år: Int): MinstebeløpResponse =
        webClient.get()
            .uri("/v2/minstebeloep/{aar}", år)
            .retrieve()
            .bodyToMono<MinstebeløpResponse>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/minstebeloep/$år")
}
