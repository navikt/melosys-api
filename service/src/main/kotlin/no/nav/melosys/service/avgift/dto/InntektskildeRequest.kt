package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class InntektskildeRequest(
    val id: UUID,
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntekt: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val erMaanedsbelop: Boolean
) {
    constructor(inntektsperioder: Inntektsperiode) : this(
        UUID.randomUUID(),
        inntektsperioder.type,
        inntektsperioder.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperioder.avgiftspliktigInntekt?.verdi,
        inntektsperioder.fomDato,
        inntektsperioder.tomDato,
        inntektsperioder.isErMaanedsbelop
    )
}
