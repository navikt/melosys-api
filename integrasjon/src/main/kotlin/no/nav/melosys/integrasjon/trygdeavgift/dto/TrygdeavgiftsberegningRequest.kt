package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.time.LocalDate


data class TrygdeavgiftsberegningRequest(
    val avgiftspliktigperioder: Set<AvgiftspliktigperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
    val foedselsdato: LocalDate?
)
