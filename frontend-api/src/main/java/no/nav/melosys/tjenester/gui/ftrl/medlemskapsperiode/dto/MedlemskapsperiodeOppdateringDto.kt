package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class MedlemskapsperiodeOppdateringDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val bestemmelse: String,
    val trygdedekning: Trygdedekninger,
    val innvilgelsesResultat: InnvilgelsesResultat
)
