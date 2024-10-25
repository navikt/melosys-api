package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.util.*

data class InntektsperiodeDto(
    val id: UUID,
    val periode: DatoPeriodeDto,
    val inntektskilde: Inntektskildetype,
    val arbeidsgiverBetalerAvgift: Boolean?,
    val månedsbeløp: PengerDto?,
    val erMaanedsbelop: Boolean,
) {
    companion object {
        fun tilInntektskilde(inntektskildeRequest: InntektsperiodeDto) = Inntektsperiode().apply {
            this.fomDato = inntektskildeRequest.periode.fom
            this.tomDato = inntektskildeRequest.periode.tom
            this.type = inntektskildeRequest.inntektskilde
            this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiverBetalerAvgift == true
            this.avgiftspliktigInntekt = inntektskildeRequest.månedsbeløp?.tilPenger()
            this.isErMaanedsbelop = inntektskildeRequest.erMaanedsbelop
        }
    }
}

data class PengerDto(val verdi: BigDecimal, var valuta: Valuta = NOK) {
    constructor(verdi: BigDecimal) : this(verdi, NOK)
    constructor(verdi: BigDecimal, valuta: String) : this(verdi, Valuta(valuta))
    constructor(penger: Penger) : this(penger.verdi, penger.valuta)

    fun tilPenger() = Penger(verdi, valuta.kode)
}

data class Valuta(val kode: String, val desimaler: Int = 2)

val NOK = Valuta("NOK", 2)
