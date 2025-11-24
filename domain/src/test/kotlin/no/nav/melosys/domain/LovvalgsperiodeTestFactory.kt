package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

fun Lovvalgsperiode.Companion.forTest(init: LovvalgsperiodeTestFactory.Builder.() -> Unit = {}): Lovvalgsperiode =
    LovvalgsperiodeTestFactory.Builder().apply(init).build()

fun lovvalgsperiodeForTest(init: LovvalgsperiodeTestFactory.Builder.() -> Unit = {}): Lovvalgsperiode =
    Lovvalgsperiode.forTest(init)

object LovvalgsperiodeTestFactory {
    val FOM: LocalDate = LocalDate.now()
    val TOM: LocalDate = LocalDate.now().plusYears(1)
    val INNVILGELSESRESULTAT = InnvilgelsesResultat.INNVILGET
    val LOVVALGSLAND = Land_iso2.NO

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var behandlingsresultat: Behandlingsresultat? = null
        var fom: LocalDate = FOM
        var tom: LocalDate? = TOM
        var lovvalgsland: Land_iso2? = LOVVALGSLAND
        var bestemmelse: LovvalgBestemmelse? = null
        var tilleggsbestemmelse: LovvalgBestemmelse? = null
        var innvilgelsesresultat: InnvilgelsesResultat = INNVILGELSESRESULTAT
        var medlemskapstype: Medlemskapstyper? = null
        var dekning: Trygdedekninger? = null
        var medlPeriodeID: Long? = null

        fun build(): Lovvalgsperiode = Lovvalgsperiode().apply {
            this.id = this@Builder.id
            this.behandlingsresultat = this@Builder.behandlingsresultat
            this.fom = this@Builder.fom
            this.tom = this@Builder.tom
            this.lovvalgsland = this@Builder.lovvalgsland
            this.bestemmelse = this@Builder.bestemmelse
            this.tilleggsbestemmelse = this@Builder.tilleggsbestemmelse
            this.innvilgelsesresultat = this@Builder.innvilgelsesresultat
            this.medlemskapstype = this@Builder.medlemskapstype
            this.dekning = this@Builder.dekning
            this.medlPeriodeID = this@Builder.medlPeriodeID
        }
    }
}
