package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import java.math.BigDecimal
import java.time.LocalDate

data class InntektskildeDto(
    val type: Inntektskildetype,
    val arbeidsgiversavgiftBetales: Boolean,
    val avgiftspliktigInntektMnd: BigDecimal?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val totalInntektForPerioden: BigDecimal?,
) {
    constructor(inntektsperiode: Inntektsperiode, totalInntektForPerioden: BigDecimal?) : this(
        inntektsperiode.type,
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        inntektsperiode.avgiftspliktigInntektMnd?.verdi,
        inntektsperiode.fomDato,
        inntektsperiode.tomDato,
        totalInntektForPerioden,
    )

    constructor(inntektsperiode: Inntektsperiode) : this(
        inntektsperiode, null
    )

    fun tilRequest(): InntektskildeRequest =
        InntektskildeRequest(type, arbeidsgiversavgiftBetales, avgiftspliktigInntektMnd, fomDato, tomDato)
}
