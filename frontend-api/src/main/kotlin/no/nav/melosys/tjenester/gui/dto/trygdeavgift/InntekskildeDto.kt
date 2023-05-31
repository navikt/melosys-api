package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import java.math.BigDecimal

data class InntekskildeDto(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntektMnd: BigDecimal?
) {
    constructor(inntektsperiode: Inntektsperiode) : this(
        inntektsperiode.type,
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperiode.avgiftspliktigInntektMnd?.verdi
    )

    fun tilRequest(): InntektskildeRequest =
        InntektskildeRequest(type, arbeidsgiversavgiftBetales, avgiftspliktigInntektMnd)
}
