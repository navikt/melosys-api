package no.nav.melosys.itest.vedtak

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.*

/**
 * Som [TrygdeavgiftsberegningTransformer], men speiler medlemskapsperiodens fom/tom og returnerer
 * én beregnet trygdeavgiftsperiode per kalenderår (slik den ekte beregningsmotoren gjør).
 * Nødvendig for tester der medlemskapsperioden går over et årsskifte.
 *
 * Ikke skattepliktig gir 6,8 % og 1000 kr/md, skattepliktig gir 0.
 */
class ÅrsdeltTrygdeavgiftsberegningTransformer : ResponseTransformerV2 {
    override fun transform(response: Response?, serveEvent: ServeEvent?): Response {
        val mapper = jacksonObjectMapper()

        if (serveEvent?.request?.url != "/api/v2/beregn") {
            throw IllegalArgumentException("Invalid url. Denne transformeren støtter kun /api/v2/beregn")
        }

        val requestBody = mapper.readTree(serveEvent.request?.bodyAsString)
        val medlemskapsperiode = requestBody["medlemskapsperioder"][0]
        val skatteforholdsperiode = requestBody["skatteforholdsperioder"][0]
        val inntektsperiode = requestBody["inntektsperioder"][0]

        val fom = LocalDate.parse(medlemskapsperiode["periode"]["fom"].asText())
        val tom = LocalDate.parse(medlemskapsperiode["periode"]["tom"].asText())

        val skatteforhold = skatteforholdsperiode["skatteforhold"].asText()
        val sats = if (skatteforhold == "IKKE_SKATTEPLIKTIG") 6.8.toBigDecimal() else 0.toBigDecimal()
        val månedsavgift = PengerDto(if (skatteforhold == "IKKE_SKATTEPLIKTIG") 1000.toBigDecimal() else 0.toBigDecimal(), NOK)
        val grunnlag = TrygdeavgiftsgrunnlagDto(
            UUID.fromString(medlemskapsperiode["id"].asText()),
            UUID.fromString(skatteforholdsperiode["id"].asText()),
            UUID.fromString(inntektsperiode["id"].asText())
        )

        val responsBody = (fom.year..tom.year).map { år ->
            TrygdeavgiftsberegningResponse(
                beregnetPeriode = TrygdeavgiftsperiodeDto(
                    DatoPeriodeDto(
                        if (fom.year == år) fom else LocalDate.of(år, 1, 1),
                        if (tom.year == år) tom else LocalDate.of(år, 12, 31)
                    ),
                    sats,
                    månedsavgift
                ),
                grunnlag = grunnlag,
                grunnlagListe = listOf(grunnlag),
                beregningsregel = Avgiftsberegningsregel.ORDINÆR
            )
        }

        return Response.Builder.like(response)
            .body(mapper.writeValueAsString(responsBody))
            .build()
    }

    override fun getName(): String = "dynamisk-trygdeavgiftsberegning-transformer"

    override fun applyGlobally(): Boolean = false
}
