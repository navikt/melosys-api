package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.avgift.model.TrygdeavgiftsgrunnlagModel

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntektskildeDto>
) {

    constructor(trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>) : this(
        trygdeavgiftsperioder.flatMap { periode ->
            if (periode.grunnlagListe.isNotEmpty()) {
                periode.grunnlagListe.map { SkatteforholdTilNorgeDto(it.skatteforhold) }
            } else {
                listOfNotNull(periode.grunnlagSkatteforholdTilNorge?.let { SkatteforholdTilNorgeDto(it) })
            }
        }.distinct(),
        trygdeavgiftsperioder.flatMap { periode ->
            if (periode.grunnlagListe.isNotEmpty()) {
                periode.grunnlagListe.map { InntektskildeDto(it.inntektsperiode) }
            } else {
                listOfNotNull(periode.grunnlagInntekstperiode?.let { InntektskildeDto(it) })
            }
        }.distinct()
    )

    constructor(grunnlagModel: TrygdeavgiftsgrunnlagModel) : this(
        grunnlagModel.skatteforholdsperioder.map { SkatteforholdTilNorgeDto(it) }.distinct(),
        grunnlagModel.inntektsperioder.map { InntektskildeDto(it) }.distinct()
    )
}
