package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgift

data class BeregnetTrygdeavgiftDto(val trygdeavgiftsperioder: Set<TrygdeavgiftsperioderDto>) {
    companion object {
        @JvmStatic
        fun av(trygdeavgiftSet: Set<Trygdeavgift>): BeregnetTrygdeavgiftDto =
            BeregnetTrygdeavgiftDto(trygdeavgiftSet.map { TrygdeavgiftsperioderDto(it) }.toSet())
    }

}
