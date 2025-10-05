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
    constructor(entity: Inntektsperiode, justertFomPeriode: LocalDate? = null) : this(
        type = entity.type,
        arbeidsgiversavgiftBetalesTilSkatt = entity.isArbeidsgiversavgiftBetalesTilSkatt,
        avgiftspliktigInntekt = (entity.avgiftspliktigMndInntekt ?: entity.avgiftspliktigTotalinntekt)?.verdi,
        fomDato = justertFomPeriode?.let { if (entity.fomDato.isBefore(it)) it else entity.fomDato } ?: entity.fomDato,
        tomDato = entity.tomDato,
        erMaanedsbelop = entity.erMaanedsbelop()
    )
}
