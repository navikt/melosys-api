package no.nav.melosys.integrasjon.trygdeavgift.dto

data class Trygdeavgiftsperiode(
    val periode: DatoPeriode,
    val sats: Double,
    val avgift: PengerDto
)
