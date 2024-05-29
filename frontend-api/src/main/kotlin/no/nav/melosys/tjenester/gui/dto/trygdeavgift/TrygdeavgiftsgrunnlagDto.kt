package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntekskildeDto>
) {

    constructor(trygdeavgiftsperiode: Set<Trygdeavgiftsperiode>) : this(
        trygdeavgiftsperiode.map { SkatteforholdTilNorgeDto(it.grunnlagSkatteforholdTilNorge) }.sortedWith(compareBy { it.tomDato }).toList(),
        trygdeavgiftsperiode.map { InntekskildeDto(it.grunnlagInntekstperiode) }.sortedWith(compareBy { it.tomDato }).toList()
    )

    fun tilRequest(): OppdaterTrygdeavgiftsgrunnlagRequest =
        OppdaterTrygdeavgiftsgrunnlagRequest((skatteforholdsperioder.map { it.tilRequest() }), (inntektskilder.map { it.tilRequest() }))
}
