package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val avgiftssats: Double,
    val avgiftPerMd: Int
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdeavgiftsperiode.hentGjeldendeTrygdedekning(),
            trygdeavgiftsperiode.trygdesats,
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.verdi.toInt()
        )
}
