package no.nav.melosys.integrasjon.trygdeavgift.dto


data class TrygdeavgiftBeregningsgrunnlagDto(
    val medlemskapsperioder: Set<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>
)
