package no.nav.melosys.integrasjon.inngangsvilkar

import no.nav.melosys.domain.ErPeriode

data class VurderInngangsvilkaarRequest(
    val statsborgerskap: Set<String>,
    val arbeidsland: Set<String>,
    val flereLandUkjentHvilke: Boolean,
    val periode: ErPeriode
)