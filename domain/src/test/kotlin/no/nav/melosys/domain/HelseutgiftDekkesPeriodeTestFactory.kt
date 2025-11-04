package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import java.time.LocalDate

fun HelseutgiftDekkesPeriode.Companion.forTest(init: HelseutgiftDekkesPeriodeTestFactory.Builder.() -> Unit = {}): HelseutgiftDekkesPeriode =
    HelseutgiftDekkesPeriodeTestFactory.Builder().apply(init).build()

object HelseutgiftDekkesPeriodeTestFactory {

    @MelosysTestDsl
    class Builder {
        var id = 0L
        var behandlingsresultat = Behandlingsresultat()
        var fomDato = LocalDate.now()
        var tomDato = LocalDate.now().plusDays(1)
        var bostedLandkode = Land_iso2.NO

        var trygdeavgiftsperioder: MutableSet<Trygdeavgiftsperiode> = mutableSetOf()

        fun trygdeavgiftsperiode(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit) {
            this@Builder.trygdeavgiftsperioder.add(
                TrygdeavgiftsperiodeTestFactory.Builder().apply(init).build()
            )
        }


        fun build(): HelseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = this.fomDato,
            tomDato = this.tomDato,
            bostedLandkode = this.bostedLandkode
        ).apply { trygdeavgiftsperioder.addAll(this@Builder.trygdeavgiftsperioder) }
    }

}
