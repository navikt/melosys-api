package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel

data class EøsPensjonistTrygdeavgiftsberegningResponse(
    val beregnetPeriode: TrygdeavgiftsperiodeDto,
    val grunnlag: EøsPensjonistTrygdeavgiftsgrunnlagDto,
    val grunnlagListe: List<EøsPensjonistTrygdeavgiftsgrunnlagDto>,
    val beregningsregel: Avgiftsberegningsregel,
)
