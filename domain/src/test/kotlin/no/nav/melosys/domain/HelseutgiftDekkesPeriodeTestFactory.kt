package no.nav.melosys.domain

import no.nav.melosys.domain.HelseutgiftDekkesPeriodeTestFactory.defaultHelseutgiftDekkesPeriode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import java.time.LocalDate

fun HelseutgiftDekkesPeriode.Companion.forTest(init: HelseutgiftDekkesPeriode.() -> Unit = {}): HelseutgiftDekkesPeriode =
    defaultHelseutgiftDekkesPeriode().apply(init)

object HelseutgiftDekkesPeriodeTestFactory {

    @JvmStatic
    fun defaultHelseutgiftDekkesPeriode() = HelseutgiftDekkesPeriode(
        behandlingsresultat = Behandlingsresultat(),
        fomDato = LocalDate.now(),
        tomDato = LocalDate.now().plusDays(1),
        bostedLandkode = Land_iso2.NO
    )
}
