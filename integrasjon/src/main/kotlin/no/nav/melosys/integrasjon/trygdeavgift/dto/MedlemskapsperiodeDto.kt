package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import java.util.*


data class MedlemskapsperiodeDto(
    val periode: DatoPeriodeDto,
    val avgiftsdekninger: Set<Avgiftsdekning>,
    var id: UUID
)
