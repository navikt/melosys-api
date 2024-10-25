package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class InntektskildeRequest(
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

    companion object {
        fun tilInntektskilde(inntektskildeRequest: InntektskildeRequest) = Inntektsperiode().apply {
            this.fomDato = inntektskildeRequest.fomDato
            this.tomDato = inntektskildeRequest.tomDato
            this.type = inntektskildeRequest.type
            this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
            this.avgiftspliktigInntekt = Penger(inntektskildeRequest.avgiftspliktigInntekt)
            this.isErMaanedsbelop = inntektskildeRequest.erMaanedsbelop
        }
    }
}
