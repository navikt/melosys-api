package no.nav.melosys.integrasjon.trygdeavgift.dto

data class EøsPensjonistTrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: EøsPensjonistTrygdeavgiftsgrunnlagDto
)
