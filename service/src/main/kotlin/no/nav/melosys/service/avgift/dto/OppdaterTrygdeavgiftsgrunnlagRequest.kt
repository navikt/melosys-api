package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.kodeverk.Skatteplikttype

class OppdaterTrygdeavgiftsgrunnlagRequest(
    val skatteplikttype: Skatteplikttype,
    val inntektskilder: List<InntektskildeRequest>
)
