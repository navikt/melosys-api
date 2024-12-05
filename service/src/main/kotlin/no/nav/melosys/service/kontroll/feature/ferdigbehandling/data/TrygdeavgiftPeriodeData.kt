package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode

data class TrygdeavgiftPeriodeData(
    val nyeTrygdeavgiftsperioder: List<Trygdeavgiftsperiode> = emptyList(),
    val tidligereTrygdeavgiftsperioder: List<Trygdeavgiftsperiode> = emptyList(),
)
