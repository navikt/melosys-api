package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

@JvmRecord
data class MedlemskapsperiodeOppdateringDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val bestemmelse: Folketrygdloven_kap2_bestemmelser,
    val trygdedekning: Trygdedekninger,
    val innvilgelsesResultat: InnvilgelsesResultat
)
