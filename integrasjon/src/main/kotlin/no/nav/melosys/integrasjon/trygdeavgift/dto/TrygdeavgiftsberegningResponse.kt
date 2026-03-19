package no.nav.melosys.integrasjon.trygdeavgift.dto

data class TrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: TrygdeavgiftsgrunnlagDto,
    val grunnlagListe: List<TrygdeavgiftsgrunnlagDto>? = null,
    val beregningstype: String? = null,
)
