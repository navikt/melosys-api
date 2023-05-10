package no.nav.melosys.integrasjon.trygdeavgift.dto

data class TrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: TrygdeavgiftsgrunnlagDto
)
