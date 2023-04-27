package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class TrygdeavgiftsperioderDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val avgiftssats: BigDecimal,
    val avgiftPerMd: BigInteger
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(trygdeavgiftsperiode.periodeFra, trygdeavgiftsperiode.periodeTil, trygdeavgiftsperiode.hentGjeldendeTrygdedekning(), trygdeavgiftsperiode.trygdesats, trygdeavgiftsperiode.trygdeavgiftsbeløpMd)
}
