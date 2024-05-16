package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import java.math.BigDecimal
import java.time.LocalDate

data class InntekskildeDto(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntektMnd: BigDecimal,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
) {
    constructor(inntektsperiode: Inntektsperiode) : this(
        inntektsperiode.type,
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperiode.avgiftspliktigInntektMnd.verdi,
        inntektsperiode.fomDato,
        inntektsperiode.tomDato
    )

    fun tilRequest(): InntektskildeRequest =
        InntektskildeRequest(type, arbeidsgiversavgiftBetales, avgiftspliktigInntektMnd, fomDato, tomDato)
}
