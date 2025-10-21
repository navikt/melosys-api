package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

fun medlemskapsperiodeForTest(init: MedlemskapsperiodeTestFactory.Builder.() -> Unit = {}): Medlemskapsperiode =
    MedlemskapsperiodeTestFactory.Builder().apply(init).build()

object MedlemskapsperiodeTestFactory {
    val FOM = LocalDate.of(2023, 1, 1)
    val TOM = LocalDate.of(2023, 12, 31)
    val INNVILGELSESRESULTAT = InnvilgelsesResultat.INNVILGET
    val MEDLEMSKAPSTYPE = Medlemskapstyper.PLIKTIG
    val TRYGDEDEKNING = Trygdedekninger.FULL_DEKNING_EOSFO
    val BESTEMMELSE: Bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var fom: LocalDate = FOM
        var tom: LocalDate = TOM
        var innvilgelsesresultat: InnvilgelsesResultat = INNVILGELSESRESULTAT
        var medlemskapstype: Medlemskapstyper = MEDLEMSKAPSTYPE
        var trygdedekning: Trygdedekninger = TRYGDEDEKNING
        var bestemmelse: Bestemmelse = BESTEMMELSE
        var medlPeriodeID: Long? = null

        private val trygdeavgiftsperioder = mutableListOf<Trygdeavgiftsperiode>()

        fun trygdeavgiftsperiode(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit) {
            trygdeavgiftsperioder.add(
                TrygdeavgiftsperiodeTestFactory.Builder().apply(init).build()
            )
        }

        fun build(): Medlemskapsperiode = Medlemskapsperiode().apply {
            this.id = this@Builder.id
            this.fom = this@Builder.fom
            this.tom = this@Builder.tom
            this.innvilgelsesresultat = this@Builder.innvilgelsesresultat
            this.medlemskapstype = this@Builder.medlemskapstype
            this.trygdedekning = this@Builder.trygdedekning
            this.bestemmelse = this@Builder.bestemmelse
            this.medlPeriodeID = this@Builder.medlPeriodeID

            // Legg til trygdeavgiftsperioder
            this@Builder.trygdeavgiftsperioder.forEach { periode ->
                this.addTrygdeavgiftsperiode(periode)
            }
        }
    }
}
