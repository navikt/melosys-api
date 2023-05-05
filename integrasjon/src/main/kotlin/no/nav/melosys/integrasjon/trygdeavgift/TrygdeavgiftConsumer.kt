package no.nav.melosys.integrasjon.trygdeavgift

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftBeregningsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*


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

    fun beregnTrygdeavgift(trygdeavgiftBeregningsgrunnlagDto: TrygdeavgiftBeregningsgrunnlagDto): List<TrygdeavgiftsperiodeDto> {
        val DBID_UUID_MAP =
            mutableListOf<BiMap<String, UUID>>(HashBiMap.create(), HashBiMap.create(), HashBiMap.create())
        konverterDBIDTilUUID(trygdeavgiftBeregningsgrunnlagDto, DBID_UUID_MAP)

        val beregnetTrygdeavgift = webClient.post()
            .uri("/v2/beregn")
            .bodyValue(trygdeavgiftBeregningsgrunnlagDto)
            .retrieve()
            .bodyToMono<List<TrygdeavgiftsperiodeDto>>()
            .block() ?: throw IllegalStateException("Ingen body fra /v2/beregn")

        konverterUUIDTilDBID(beregnetTrygdeavgift, DBID_UUID_MAP)
        return beregnetTrygdeavgift
    }

    private fun konverterDBIDTilUUID(
        trygdeavgiftBeregningsgrunnlagDto: TrygdeavgiftBeregningsgrunnlagDto,
        DBID_UUID_MAP: MutableList<BiMap<String, UUID>>
    ) {
        trygdeavgiftBeregningsgrunnlagDto.inntektsperioder.forEach {
            it.id = DBID_UUID_MAP.get(0).put(it.id, UUID.randomUUID()).toString()
        }
        trygdeavgiftBeregningsgrunnlagDto.medlemskapsperioder.forEach {
            it.id = DBID_UUID_MAP.get(1).put(it.id, UUID.randomUUID()).toString()
        }
        trygdeavgiftBeregningsgrunnlagDto.skatteforholdsperioder.forEach {
            it.id = DBID_UUID_MAP.get(2).put(it.id, UUID.randomUUID()).toString()
        }
    }

    private fun konverterUUIDTilDBID(
        trygdeavgiftsperioderDto: List<TrygdeavgiftsperiodeDto>,
        DBID_UUID_MAP: MutableList<BiMap<String, UUID>>
    ) {
        trygdeavgiftsperioderDto.forEach {
            it.grunnlagInntektsperiode = DBID_UUID_MAP.get(0).inverse().get(UUID.fromString(it.grunnlagInntektsperiode))
                ?: throw TekniskException("")
            it.grunnlagMedlemskapsperiode = DBID_UUID_MAP.get(1).inverse().get(UUID.fromString(it.grunnlagMedlemskapsperiode))
                ?: throw TekniskException("")
            it.grunnlagSkatteforholdsperiode = DBID_UUID_MAP.get(2).inverse().get(UUID.fromString(it.grunnlagSkatteforholdsperiode))
                ?: throw TekniskException("")
        }
    }

}
