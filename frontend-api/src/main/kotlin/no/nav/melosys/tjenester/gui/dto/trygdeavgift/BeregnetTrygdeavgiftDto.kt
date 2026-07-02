package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.dto.BeregningsforklaringDto

data class BeregnetTrygdeavgiftDto(
    val trygdeavgiftsperioder: List<TrygdeavgiftsperiodeDto>,
    val trygdeavgiftsgrunnlag: TrygdeavgiftsgrunnlagDto,
    // Føres kun gjennom på PUT-veien (beregning). GET gir alltid tom liste siden
    // forklaringen ikke persisteres.
    val beregningsforklaringer: List<BeregningsforklaringDto> = emptyList(),
) {
    companion object {
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>) = BeregnetTrygdeavgiftDto(
            trygdeavgiftsperiodeSet.map { TrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom }),
            TrygdeavgiftsgrunnlagDto(trygdeavgiftsperiodeSet)
        )
    }
}
