package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import java.time.LocalDate

data class EøsPensjonistTrygdeavgiftsberegningRequest(
    val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriodeDto,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
    val foedselsdato: LocalDate?
)
