package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Skatteforhold og inntekter som er lagt til grunn for trygdeavgiften, vist i vedlegget «Avgiftsgrunnlag».
 */
data class Avgiftsgrunnlag(
    val skatteforholdsperioder: List<Skatteforholdsperiode>,
    val inntektsperioder: List<Inntektsperiode>,
    val visArbeidsgiversavgift: Boolean,
) {
    data class Skatteforholdsperiode(
        @JsonSerialize(using = LocalDateSerializer::class)
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        val fom: LocalDate,
        @JsonSerialize(using = LocalDateSerializer::class)
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        val tom: LocalDate?,
        val skatteplikttype: Skatteplikttype,
    )

    data class Inntektsperiode(
        @JsonSerialize(using = LocalDateSerializer::class)
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        val fom: LocalDate,
        @JsonSerialize(using = LocalDateSerializer::class)
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        val tom: LocalDate?,
        val inntektskildetype: Inntektskildetype,
        val arbeidsgiversavgiftBetalesTilSkatt: Boolean?,
        val avgiftspliktigInntekt: BigDecimal?,
        val erMaanedsbelop: Boolean,
    )
}
