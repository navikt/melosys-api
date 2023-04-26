package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.math.BigDecimal

data class Trygdeavgiftsperiode(
    val periode: DatoPeriode,
    val sats: BigDecimal,
    val avgift: Penger
)
