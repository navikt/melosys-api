package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class BeregnetTrygdeavgiftDto(
    val trygdeavgiftsperioder: List<TrygdeavgiftsperiodeDto>,
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
) {
    companion object {
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>, trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto) = BeregnetTrygdeavgiftDto(
            trygdeavgiftsperiodeSet.map { TrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom }),
            trygdeavgiftsgrunnlag
        )
    }
}
