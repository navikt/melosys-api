package no.nav.melosys.integrasjon.trygdeavgift.dto

import java.time.LocalDate


data class TrygdeavgiftsberegningRequest(
    val medlemskapsperioder: Set<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
    val foedselsdato: LocalDate?
)
