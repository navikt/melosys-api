package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class BeregnetTrygdeavgiftDto(val trygdeavgiftsperioder: Set<TrygdeavgiftsperioderDto>) {
    companion object {
        @JvmStatic
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>): BeregnetTrygdeavgiftDto =
            BeregnetTrygdeavgiftDto(trygdeavgiftsperiodeSet.map { TrygdeavgiftsperioderDto(it) }.toSet())
    }
}
