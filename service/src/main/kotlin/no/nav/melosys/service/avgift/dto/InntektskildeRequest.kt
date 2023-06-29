package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal

class InntektskildeRequest(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntektMnd: BigDecimal?
) {
    constructor(inntektsperioder: Inntektsperiode) : this(
        inntektsperioder.type,
        inntektsperioder.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperioder.avgiftspliktigInntektMnd?.verdi
    )
}
