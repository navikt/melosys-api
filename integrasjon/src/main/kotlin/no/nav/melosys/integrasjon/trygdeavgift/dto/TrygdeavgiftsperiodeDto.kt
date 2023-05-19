package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.math.BigDecimal

data class TrygdeavgiftsperiodeDto(
    val periode: DatoPeriodeDto,
    val sats: BigDecimal,
    val månedsavgift: PengerDto,
)
