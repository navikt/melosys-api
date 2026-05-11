package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.math.BigDecimal

data class MinstebelopResponse(
    val aar: Int,
    val beloep: BigDecimal
)
