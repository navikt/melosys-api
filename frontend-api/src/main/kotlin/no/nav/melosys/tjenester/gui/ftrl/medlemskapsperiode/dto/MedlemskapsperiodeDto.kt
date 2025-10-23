package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto

import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class MedlemskapsperiodeDto(
    val id: Long,
    override val periodeFra: LocalDate,
    override val periodeTil: LocalDate,
    val bestemmelse: Bestemmelse?,
    val innvilgelsesResultat: InnvilgelsesResultat,
    override val dekning: Trygdedekninger,
    val medlemskapstype: Medlemskapstyper?,
) : AvgiftspliktigPeriode {
    override fun erInnvilget(): Boolean {
        return innvilgelsesResultat == InnvilgelsesResultat.INNVILGET
    }

    override fun getFom(): LocalDate {
        return periodeFra
    }

    override fun getTom(): LocalDate {
        return periodeTil
    }
}
