package no.nav.melosys.integrasjon.trygdeavgift.dto


data class TrygdeavgiftsberegningRequest(
    val medlemskapsperioder: Set<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>
)
