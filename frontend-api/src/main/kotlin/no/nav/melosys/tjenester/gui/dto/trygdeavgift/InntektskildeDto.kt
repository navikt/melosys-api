package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.service.avgift.model.InntektsperiodeModel
import java.math.BigDecimal
import java.time.LocalDate

data class InntektskildeDto(
    val type: Inntektskildetype,
    @JsonSetter(nulls = Nulls.SKIP)
    val arbeidsgiversavgiftBetales: Boolean = false,
    val avgiftspliktigInntekt: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    @JsonSetter(nulls = Nulls.SKIP)
    val erMaanedsbelop: Boolean = false,
) {
    constructor(inntektsperiode: Inntektsperiode) : this(
        inntektsperiode.type,
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        (inntektsperiode.avgiftspliktigMndInntekt ?: inntektsperiode.avgiftspliktigTotalinntekt)?.verdi
            ?: throw IllegalStateException("avgiftspliktigMndInntekt og avgiftspliktigTotalinntekt er null"),
        inntektsperiode.fomDato,
        inntektsperiode.tomDato,
        inntektsperiode.erMaanedsbelop(),
    )

    constructor(model: InntektsperiodeModel) : this(
        model.type,
        model.arbeidsgiversavgiftBetalesTilSkatt,
        model.avgiftspliktigInntekt,
        model.fomDato,
        model.tomDato,
        model.erMaanedsbelop
    )
}
