package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel

data class TrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: TrygdeavgiftsgrunnlagDto,
    val grunnlagListe: List<TrygdeavgiftsgrunnlagDto>,
    val beregningsregel: Avgiftsberegningsregel,
    val avgiftsdel: String? = null,
)
