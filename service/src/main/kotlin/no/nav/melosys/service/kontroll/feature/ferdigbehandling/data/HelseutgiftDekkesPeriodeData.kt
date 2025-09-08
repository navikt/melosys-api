package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode

data class HelseutgiftDekkesPeriodeData(
    val nyHelseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
    val tidligereHelseutgiftDekkesPerioder: List<HelseutgiftDekkesPeriode> = emptyList(),
)
