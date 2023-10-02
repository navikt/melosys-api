package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntekskildeDto>
) {
    constructor(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag) : this(
        (trygdeavgiftsgrunnlag.skatteforholdTilNorge.map { SkatteforholdTilNorgeDto(it) }),
        (trygdeavgiftsgrunnlag.inntektsperioder.map { InntekskildeDto(it) })
    )

    fun tilRequest(): OppdaterTrygdeavgiftsgrunnlagRequest =
        OppdaterTrygdeavgiftsgrunnlagRequest((skatteforholdsperioder.map { it.tilRequest() }), (inntektskilder.map { it.tilRequest() }))
}
