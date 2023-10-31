package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class BeregnetTrygdeavgiftDto(val trygdeavgiftsperioder: List<TrygdeavgiftsperiodeDto>, val fakturamottaker: String) {
    companion object {
        fun av(trygdeavgiftsperiodeSet: Set<Trygdeavgiftsperiode>, fakturamottaker: String): BeregnetTrygdeavgiftDto =
            BeregnetTrygdeavgiftDto(trygdeavgiftsperiodeSet.map { TrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom }), fakturamottaker)
    }
}
