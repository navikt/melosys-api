package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Avgiftsdekning


data class MedlemskapsperiodeDto(
    val periode: DatoPeriodeDto,
    val avgiftsdekninger: Set<Avgiftsdekning>,
    var id: String
)
