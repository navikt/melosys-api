package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Avgiftsdekning


data class Medlemskapsperiode(
    val periode: DatoPeriode,
    val avgiftsdekninger: Set<Avgiftsdekning>
)
