package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import java.util.UUID

data class HelseutgiftDekkesPeriodeDto(
    val id: UUID,
    val periode: DatoPeriodeDto,
)
