package no.nav.melosys.itest.vedtak

import tools.jackson.module.kotlin.jacksonObjectMapper
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
class EøsPensjonistTrygdeavgiftsberegningTransformer : ResponseTransformerV2 {
    override fun transform(response: Response?, serveEvent: ServeEvent?): Response {

        val mapper = jacksonObjectMapper()

        if (serveEvent?.request?.url != "/api/v2/eos-pensjonist/beregn") {
            throw IllegalArgumentException("Invalid url. Denne transformeren støtter kun /api/v2/eos-pensjonist/beregn")
        }

        val requestBody = mapper.readTree(serveEvent.request?.bodyAsString)

        try {
            val helseutgiftDekkesPeriodeFom = requestBody["helseutgiftDekkesPeriode"]["periode"]["fom"]
            val helseutgiftDekkesPeriodeTom = requestBody["helseutgiftDekkesPeriode"]["periode"]["tom"]

            val mappetDatoFom = LocalDate.parse(helseutgiftDekkesPeriodeFom.asText())
            val mappetDatoTom = LocalDate.parse(helseutgiftDekkesPeriodeTom.asText())
            val skatteforholdsperioderUuid = requestBody["skatteforholdsperioder"][0]["id"].asText()
            val inntektsperioderUuid = requestBody["inntektsperioder"][0]["id"].asText()

            val skatteforhold = requestBody["skatteforholdsperioder"][0]["skatteforhold"].asText()
            val sats = if (skatteforhold == "IKKE_SKATTEPLIKTIG") 6.8.toBigDecimal() else 0.toBigDecimal()
            val månedsavgift = if (skatteforhold == "IKKE_SKATTEPLIKTIG") PengerDto(
                1000.toBigDecimal(),
                NOK
            ) else PengerDto(0.toBigDecimal(), NOK)

            val grunnlag = EøsPensjonistTrygdeavgiftsgrunnlagDto(
                DatoPeriodeDto(fom = mappetDatoFom, tom = mappetDatoTom),
                UUID.fromString(skatteforholdsperioderUuid),
                UUID.fromString(inntektsperioderUuid)
            )
            val responsBodyFraTrygdeavgiftsberegning = listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    beregnetPeriode = TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                        sats,
                        månedsavgift
                    ),
                    grunnlag = grunnlag,
                    grunnlagListe = listOf(grunnlag),
                    beregningsregel = "ORDINÆR"
                )
            )

            return Response.Builder.like(response)
                .body(mapper.writeValueAsString(responsBodyFraTrygdeavgiftsberegning))
                .build()
        } catch (exception: Exception ) {
            println(exception)
        }

        return Response.Builder().build()
    }


    override fun getName(): String {
        return "dynamisk-trygdeavgiftsberegning-eos-pensjonist-transformer"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}
