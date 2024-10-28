package no.nav.melosys.service.avgift.dto

// TODO rydd opp
class OppdaterTrygdeavgiftsgrunnlagRequest(
    val skatteforholdTilNorgeList: List<SkatteforholdTilNorgeRequest>,
    val inntektskilder: List<InntektskildeRequest>
)
