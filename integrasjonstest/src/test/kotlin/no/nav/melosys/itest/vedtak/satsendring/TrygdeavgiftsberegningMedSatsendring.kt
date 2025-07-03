import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import mu.KotlinLogging
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.itest.vedtak.satsendring.SatsendringIT.Companion.GAMMEL_SATS
import no.nav.melosys.itest.vedtak.satsendring.SatsendringIT.Companion.NY_SATS
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class TrygdeavgiftsberegningMedSatsendring : ResponseTransformerV2 {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)


    private val kallPerMedlemskapsperiode = ConcurrentHashMap<String, Int>()

    override fun transform(response: Response?, serveEvent: ServeEvent): Response {
        require(serveEvent.request?.url == "/api/v2/beregn") {
            "Invalid url. Denne transformeren støtter kun /api/v2/beregn"
        }

        val requestBody = objectMapper.readTree(serveEvent.request?.bodyAsString)

        val responseBody = createResponseBody(requestBody)
        logger.debug { "Transformed Response Body: $responseBody" }

        return Response.Builder.like(response)
            .body(responseBody)
            .build()
    }

    private fun createResponseBody(requestBody: JsonNode): String {
        val periodeString = medlemskapsperiodeStringFrom(requestBody)
        val antallKall = kallPerMedlemskapsperiode.getOrDefault(periodeString, 0) + 1
        kallPerMedlemskapsperiode[periodeString] = antallKall
        logger.debug { "Kall per medlemskapsperiode: $periodeString -> $antallKall" }

        val skatteforhold = requestBody["skatteforholdsperioder"][0]["skatteforhold"].asText()
        val sats = bestemSats(skatteforhold, periodeString, antallKall)
        val månedsavgift = sats * 10000

        val trygdeavgiftsberegningResponse =
            TrygdeavgiftsberegningResponse(
                TrygdeavgiftsperiodeDto(
                    DatoPeriodeDto(localDateFromRequest("fom", requestBody), localDateFromRequest("tom", requestBody)),
                    sats.toBigDecimal(),
                    PengerDto(månedsavgift.toBigDecimal(), NOK)
                ),
                TrygdeavgiftsgrunnlagDto(
                    UUID.fromString(requestBody["medlemskapsperioder"][0]["id"].asText()),
                    UUID.fromString(requestBody["skatteforholdsperioder"][0]["id"].asText()),
                    UUID.fromString(requestBody["inntektsperioder"][0]["id"].asText())
                )
            )

        return objectMapper.writeValueAsString(listOf(trygdeavgiftsberegningResponse))
    }

    private fun bestemSats(skatteforhold: String?, periodeString: String, antallKall: Int): Double {
        if (skatteforhold == "SKATTEPLIKTIG") return 0.0

        return if (periodeString == "2024-04-01 / 2024-04-30" || periodeString == "2024-05-01 / 2024-05-31") {
            // Perioder med satsendring
            when (antallKall) {
                1 -> GAMMEL_SATS
                else -> NY_SATS
            }
        } else {
            8.3
        }
    }

    private fun medlemskapsperiodeStringFrom(requestBody: JsonNode): String {
        val fom = localDateFromRequest("fom", requestBody)
        val tom = localDateFromRequest("tom", requestBody)
        return "$fom / $tom"
    }

    private fun localDateFromRequest(datoID: String, requestBody: JsonNode): LocalDate =
        requestBody["medlemskapsperioder"][0]["periode"][datoID]
            .map { it.asInt() }
            .let { (year, month, day) -> LocalDate.of(year, month, day) }

    override fun getName(): String {
        return "trygdeavgiftsberegning-med-satsendring-transformer"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}
