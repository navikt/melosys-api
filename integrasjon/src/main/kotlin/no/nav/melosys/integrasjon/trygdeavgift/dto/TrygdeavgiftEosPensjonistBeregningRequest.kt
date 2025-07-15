package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import java.time.LocalDate

data class TrygdeavgiftEosPensjonistBeregningRequest(
    val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
)
