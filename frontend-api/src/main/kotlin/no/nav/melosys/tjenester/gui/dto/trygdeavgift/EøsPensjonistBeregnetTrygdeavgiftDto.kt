package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.dto.BeregningsforklaringDto

data class EøsPensjonistBeregnetTrygdeavgiftDto(
    val trygdeavgiftsperioder: List<EøsPensjonistTrygdeavgiftsperiodeDto>,
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    // Føres kun gjennom på PUT; GET gir tom liste fordi forklaringen ikke persisteres.
    val beregningsforklaringer: List<BeregningsforklaringDto> = emptyList(),
) {
    companion object {
        fun av(
            trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>,
            beregningsforklaringer: List<BeregningsforklaringDto> = emptyList(),
        ) = EøsPensjonistBeregnetTrygdeavgiftDto(
            trygdeavgiftsperiodeSet.map { EøsPensjonistTrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom }),
            TrygdeavgiftsgrunnlagDto(trygdeavgiftsperiodeSet),
            beregningsforklaringer,
        )
    }
}
