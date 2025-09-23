package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import java.time.LocalDate
import java.util.*

/**
 * Wiremock transformer for å simulere dynamisk respons fra trygdeavgiftsberegning. I produksjonskoden settes det UUID.randomUUID() for id-ene, som
 * returneres i responsen til trygdeavgiftsberegning. Derfor må denne transformeren settes opp for å returnere UUID-ene som forventes i responsen.
 */
class TrygdeavgiftsberegningTransformer(private val dato: LocalDate) : ResponseTransformerV2 {
    override fun transform(response: Response?, serveEvent: ServeEvent?): Response {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        if (serveEvent?.request?.url != "/api/v2/beregn") {
            throw IllegalArgumentException("Invalid url. Denne transformeren støtter kun /api/v2/beregn")
        }

        val requestBody = mapper.readTree(serveEvent.request?.bodyAsString)
        val medlemskapsperioderUuid = requestBody["medlemskapsperioder"][0]["id"].asText()
        val skatteforholdsperioderUuid = requestBody["skatteforholdsperioder"][0]["id"].asText()
        val inntektsperioderUuid = requestBody["inntektsperioder"][0]["id"].asText()

        val skatteforhold = requestBody["skatteforholdsperioder"][0]["skatteforhold"].asText()
        val sats = if (skatteforhold == "IKKE_SKATTEPLIKTIG") 6.8.toBigDecimal() else 0.toBigDecimal()
        val månedsavgift = if (skatteforhold == "IKKE_SKATTEPLIKTIG") PengerDto(
            1000.toBigDecimal(),
            NOK
        ) else PengerDto(0.toBigDecimal(), NOK)
        val responsBodyFraTrygdeavgiftsberegning = listOf(
            TrygdeavgiftsberegningResponse(
                TrygdeavgiftsperiodeDto(
                    DatoPeriodeDto(LocalDate.of(dato.year, 1, 1), LocalDate.of(dato.year, 2, 1)),
                    sats,
                    månedsavgift
                ),
                TrygdeavgiftsgrunnlagDto(
                    UUID.fromString(medlemskapsperioderUuid),
                    UUID.fromString(skatteforholdsperioderUuid),
                    UUID.fromString(inntektsperioderUuid)
                )
            )
        )

        return Response.Builder.like(response)
            .body(mapper.writeValueAsString(responsBodyFraTrygdeavgiftsberegning))
            .build()
    }


    override fun getName(): String {
        return "dynamisk-trygdeavgiftsberegning-transformer"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}
