package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Skatteplikttype

data class SkatteforholdsperiodeDto(
    val periode: DatoPeriodeDto,
    val skatteforhold: Skatteplikttype,
)
