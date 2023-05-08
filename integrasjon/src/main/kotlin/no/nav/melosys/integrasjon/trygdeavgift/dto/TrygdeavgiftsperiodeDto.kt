package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.util.*

data class TrygdeavgiftsperiodeDto(
    val periode: DatoPeriodeDto,
    val sats: Double,
    val avgift: PengerDto,
    var grunnlagInntektsperiode: UUID,
    var grunnlagMedlemskapsperiode: UUID,
    var grunnlagSkatteforholdsperiode: UUID,
)
