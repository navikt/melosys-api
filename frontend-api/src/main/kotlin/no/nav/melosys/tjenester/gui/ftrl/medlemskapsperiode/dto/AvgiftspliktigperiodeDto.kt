package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto

import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class AvgiftspliktigperiodeDto(
    val id: Long,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val bestemmelse: Bestemmelse? = null,
    val innvilgelsesResultat: InnvilgelsesResultat? = null,
    val dekning: Trygdedekninger,
    val medlemskapstyper: Medlemskapstyper? = null,
)
