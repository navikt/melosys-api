package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate

class InntektskildeRequest(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntektMnd: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
) {
    constructor(inntektsperioder: Inntektsperiode) : this(
        inntektsperioder.type,
        inntektsperioder.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperioder.avgiftspliktigInntekt?.verdi,
        inntektsperioder.fomDato,
        inntektsperioder.tomDato
    )
}
