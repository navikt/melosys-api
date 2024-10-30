package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate

data class InntektskildeDto(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntekt: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val erMaanedsbelop: Boolean,
) {
    constructor(inntektsperiode: Inntektsperiode) : this(
        inntektsperiode.type,
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperiode.avgiftspliktigInntekt?.verdi,
        inntektsperiode.fomDato,
        inntektsperiode.tomDato,
        inntektsperiode.isErMaanedsbelop,
    )

    companion object {
        fun List<InntektskildeDto>.tilInntektsPerioder() = map { inntektsperiode ->
            Inntektsperiode().apply {
                fomDato = inntektsperiode.fomDato
                tomDato = inntektsperiode.tomDato
                type = inntektsperiode.type
                isArbeidsgiversavgiftBetalesTilSkatt = inntektsperiode.arbeidsgiversavgiftBetales
                avgiftspliktigInntekt = Penger(inntektsperiode.avgiftspliktigInntekt ?: 0.toBigDecimal())
                isErMaanedsbelop = inntektsperiode.erMaanedsbelop
            }
        }
    }
}
