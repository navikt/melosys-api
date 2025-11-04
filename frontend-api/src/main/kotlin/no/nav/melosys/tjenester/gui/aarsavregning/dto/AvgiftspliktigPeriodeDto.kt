package no.nav.melosys.tjenester.gui.aarsavregning.dto

import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class AvgiftspliktigPeriodeDto(
    val id: Long,
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val bestemmelse: Bestemmelse?,
    val innvilgelsesResultat: InnvilgelsesResultat?,
    val trygdedekning: Trygdedekninger?,
    val medlemskapstype: Medlemskapstyper?
)
