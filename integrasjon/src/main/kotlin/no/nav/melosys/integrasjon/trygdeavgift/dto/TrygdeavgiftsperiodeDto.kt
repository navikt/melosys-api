package no.nav.melosys.integrasjon.trygdeavgift.dto

data class TrygdeavgiftsperiodeDto(
    val periode: DatoPeriodeDto,
    val sats: Double,
    val månedsavgift: PengerDto,
)
