package no.nav.melosys.tjenester.gui.ftrl.helseutgiftDekkesPeriode.dto

import no.nav.melosys.domain.avgift.Fastsettingsperiode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class HelseutgiftDekkesPeriodeDto(
    val id: Long,
    override val periodeFra: LocalDate,
    override val periodeTil: LocalDate,
    override val dekning: Trygdedekninger,
) : Fastsettingsperiode {
    override fun erInnvilget(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFom(): LocalDate {
        return periodeFra
    }

    override fun getTom(): LocalDate {
        return periodeTil
    }
}
