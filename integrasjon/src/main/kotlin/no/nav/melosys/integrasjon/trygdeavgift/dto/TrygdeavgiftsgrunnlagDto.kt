package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.util.*

data class TrygdeavgiftsgrunnlagDto(
    val avgiftspliktigperiodeId: UUID,
    val skatteforholdsperiodeId: UUID,
    val inntektsperiodeId: UUID,
)
