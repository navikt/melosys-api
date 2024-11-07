package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntektskildeDto>
) {

    constructor(trygdeavgiftsperiode: Set<Trygdeavgiftsperiode>) : this(
        trygdeavgiftsperiode.map { SkatteforholdTilNorgeDto(it.grunnlagSkatteforholdTilNorge) }.distinct(),
        trygdeavgiftsperiode
            .filter { it.grunnlagInntekstperiode != null }
            .map { InntektskildeDto(it.grunnlagInntekstperiode) }
            .distinct()
    )
}
