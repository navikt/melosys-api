package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.time.LocalDate

data class EøsPensjonistTrygdeavgiftsberegningRequest(
    val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriodeDto,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
    val foedselsdato: LocalDate?
)
