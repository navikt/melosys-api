package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class MedlemskapsperiodeDto(
    val id: Long,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val bestemmelse: Bestemmelse,
    val innvilgelsesResultat: InnvilgelsesResultat,
    val trygdedekning: Trygdedekninger,
    val medlemskapstype: Medlemskapstyper
) {
    companion object {
        fun av(medlemskapsperiode: Medlemskapsperiode): MedlemskapsperiodeDto {
            return MedlemskapsperiodeDto(
                medlemskapsperiode.id,
                medlemskapsperiode.fom,
                medlemskapsperiode.tom,
                medlemskapsperiode.bestemmelse,
                medlemskapsperiode.innvilgelsesresultat,
                medlemskapsperiode.trygdedekning,
                medlemskapsperiode.medlemskapstype
            )
        }
    }
}
