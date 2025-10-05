package no.nav.melosys.service.avgift.model

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate

data class InntektsperiodeModel(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetalesTilSkatt: Boolean,
    val avgiftspliktigInntekt: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val erMaanedsbelop: Boolean
) {
    companion object {
        fun fromEntity(inntektsperiode: Inntektsperiode): InntektsperiodeModel {
            return InntektsperiodeModel(
                type = inntektsperiode.type,
                arbeidsgiversavgiftBetalesTilSkatt = inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                avgiftspliktigInntekt = (inntektsperiode.avgiftspliktigMndInntekt ?: inntektsperiode.avgiftspliktigTotalinntekt)?.verdi,
                fomDato = inntektsperiode.fomDato,
                tomDato = inntektsperiode.tomDato,
                erMaanedsbelop = inntektsperiode.erMaanedsbelop()
            )
        }
    }
}
