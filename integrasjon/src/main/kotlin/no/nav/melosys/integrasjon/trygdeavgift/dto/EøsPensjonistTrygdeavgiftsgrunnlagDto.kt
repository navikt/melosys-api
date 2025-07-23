package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.util.UUID

data class EøsPensjonistTrygdeavgiftsgrunnlagDto(
    val helseutgiftDekkesPeriode: DatoPeriodeDto,
    val skatteforholdsperiodeId: UUID,
    val inntektsperiodeId: UUID
)
