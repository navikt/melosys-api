package no.nav.melosys.integrasjon.trygdeavgift.dto

data class TrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: TrygdeavgiftsgrunnlagDto,
    val grunnlagListe: List<TrygdeavgiftsgrunnlagDto>,
    val beregningsregel: String,
    val avgiftsdel: String? = null,
)
