package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.math.BigDecimal

data class MinstebeløpResponse(
    val aar: Int,
    val beloep: BigDecimal
)
