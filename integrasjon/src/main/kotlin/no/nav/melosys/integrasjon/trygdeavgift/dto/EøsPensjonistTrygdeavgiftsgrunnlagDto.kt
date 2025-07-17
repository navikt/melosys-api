package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.util.UUID

data class EøsPensjonistTrygdeavgiftsgrunnlagDto(
    val helseutgiftDekkesPeriodeId: UUID,
    val skatteforholdsperiodeId: UUID,
    val inntektsperiodeId: UUID
)
