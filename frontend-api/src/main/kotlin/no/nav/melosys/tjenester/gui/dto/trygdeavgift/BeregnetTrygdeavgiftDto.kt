package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class BeregnetTrygdeavgiftDto(val trygdeavgiftsperioder: Set<TrygdeavgiftsperiodeDto>) {
    companion object {
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>): BeregnetTrygdeavgiftDto =
            BeregnetTrygdeavgiftDto(trygdeavgiftsperiodeSet.map { TrygdeavgiftsperiodeDto(it) }.toSet())
    }
}
