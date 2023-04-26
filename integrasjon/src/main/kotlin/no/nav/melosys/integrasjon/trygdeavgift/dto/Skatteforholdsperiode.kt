package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Skatteplikttype

data class Skatteforholdsperiode(
    val periode: DatoPeriode,
    val skatteforhold: Skatteplikttype,
)
