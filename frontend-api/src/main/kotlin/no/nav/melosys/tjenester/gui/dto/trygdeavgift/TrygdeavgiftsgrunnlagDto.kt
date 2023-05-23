package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest

data class TrygdeavgiftsgrunnlagDto(
    val skatteplikttype: Skatteplikttype,
    val inntektskilder: List<InntekskildeDto>
) {
    constructor(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag) : this(
        trygdeavgiftsgrunnlag.skatteforholdTilNorge.first().skatteplikttype,
        (trygdeavgiftsgrunnlag.inntektsperioder.map { InntekskildeDto(it) })
    )

    fun tilRequest(): OppdaterTrygdeavgiftsgrunnlagRequest =
        OppdaterTrygdeavgiftsgrunnlagRequest(skatteplikttype, (inntektskilder.map { it.tilRequest() }))
}
