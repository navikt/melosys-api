package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest

data class TrygdeavgiftsgrunnlagDto(
    val skatteforholdsperioder: Set<SkatteforholdTilNorgeDto>,
    val inntektskilder: Set<InntekskildeDto>
) {

    constructor(trygdeavgiftsperiode: Set<Trygdeavgiftsperiode>) : this(
        trygdeavgiftsperiode.map { SkatteforholdTilNorgeDto(it.grunnlagSkatteforholdTilNorge) }.toSet(),
        trygdeavgiftsperiode.map { InntekskildeDto(it.grunnlagInntekstperiode) }.toSet()
    )

    fun tilRequest(): OppdaterTrygdeavgiftsgrunnlagRequest =
        OppdaterTrygdeavgiftsgrunnlagRequest((skatteforholdsperioder.map { it.tilRequest() }), (inntektskilder.map { it.tilRequest() }))
}
