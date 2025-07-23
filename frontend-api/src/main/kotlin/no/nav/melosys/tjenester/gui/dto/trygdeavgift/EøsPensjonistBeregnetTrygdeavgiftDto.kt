package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class EøsPensjonistBeregnetTrygdeavgiftDto(
    val trygdeavgiftsperioder: List<EøsPensjonistTrygdeavgiftsperiodeDto>,
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
) {
    companion object {
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>) = EøsPensjonistBeregnetTrygdeavgiftDto(
            trygdeavgiftsperiodeSet.map { EøsPensjonistTrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom }),
            TrygdeavgiftsgrunnlagDto(trygdeavgiftsperiodeSet)
        )
    }
}
