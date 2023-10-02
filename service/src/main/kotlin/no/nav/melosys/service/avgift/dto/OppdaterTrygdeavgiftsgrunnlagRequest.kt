package no.nav.melosys.service.avgift.dto

class OppdaterTrygdeavgiftsgrunnlagRequest(
    val skatteforholdTilNorgeList: List<SkatteforholdTilNorgeRequest>,
    val inntektskilder: List<InntektskildeRequest>
)
