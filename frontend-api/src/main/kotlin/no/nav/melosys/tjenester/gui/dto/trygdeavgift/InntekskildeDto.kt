package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektskilde
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import java.math.BigInteger

data class InntekskildeDto(
    val type: Inntektskildetype,
    val avgiftspliktigInntektMnd: BigInteger,
    val arbeidsgiversavgiftBetales: Boolean
) {

    fun tilRequest(): InntektskildeRequest =
        InntektskildeRequest(type, avgiftspliktigInntektMnd, arbeidsgiversavgiftBetales)

    companion object {
        fun av(inntektskilde: Inntektskilde): InntekskildeDto =
            InntekskildeDto(inntektskilde.inntektskildetype, inntektskilde.avgiftspliktigInntektMnd, inntektskilde.isArbeidsgiversavgiftBetalesTilSkatt)
    }
}
