package no.nav.melosys.service.avgift.model

data class TrygdeavgiftsgrunnlagModel(
    val skatteforholdsperioder: List<SkatteforholdTilNorgeModel>,
    val inntektsperioder: List<InntektsperiodeModel>
)
