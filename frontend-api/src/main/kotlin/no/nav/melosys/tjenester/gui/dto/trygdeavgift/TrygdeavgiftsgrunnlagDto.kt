package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.service.avgift.model.TrygdeavgiftsgrunnlagModel

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntektskildeDto>
) {

    constructor(grunnlagModel: TrygdeavgiftsgrunnlagModel) : this(
        grunnlagModel.skatteforholdsperioder.map { SkatteforholdTilNorgeDto(it) }.distinct(),
        grunnlagModel.inntektsperioder.map { InntektskildeDto(it) }.distinct()
    )
}
