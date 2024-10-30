package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalBeløpBeregner
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

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
        fun List<InntektskildeDto>.tilInntektsPeriodeDtos(): List<InntektsperiodeDto> {
            return map {
                val avgiftsPliktigInntekt = if (it.erMaanedsbelop) it.avgiftspliktigInntekt else TotalBeløpBeregner.månedligBeløpForTotalbeløp(
                    it.fomDato,
                    it.tomDato, it.avgiftspliktigInntekt!!
                )

                InntektsperiodeDto(
                    id = UUID.randomUUID(), // TODO fix
                    periode = DatoPeriodeDto(it.fomDato, it.tomDato),
                    inntektskilde = it.type,
                    arbeidsgiverBetalerAvgift = it.arbeidsgiversavgiftBetales,
                    månedsbeløp = PengerDto(avgiftsPliktigInntekt ?: 0.toBigDecimal()),
                    erMaanedsbelop = true
                )
            }
        }
    }
}
