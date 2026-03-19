package no.nav.melosys.integrasjon.trygdeavgift.dto

data class EøsPensjonistTrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: EøsPensjonistTrygdeavgiftsgrunnlagDto,
    val grunnlagListe: List<EøsPensjonistTrygdeavgiftsgrunnlagDto>? = null,
    val beregningstype: String? = null,
)
