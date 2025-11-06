package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val inntektskildetype: Inntektskildetype?,
    val avgiftssats: Double,
    val avgiftPerMd: Int
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdedekning = if (trygdeavgiftsperiode.grunnlagMedlemskapsperiode != null) {
                trygdeavgiftsperiode.grunnlagMedlemskapsperiodeNotNull.hentTrygdedekning()
            } else if (trygdeavgiftsperiode.grunnlagLovvalgsPeriode != null) {
                 trygdeavgiftsperiode.grunnlagLovvalgsPeriodeNotNull.hentTrygdedekning()
            } else {
                throw IllegalStateException("Mangler grunnlag for trygdedekning")
            },
            trygdeavgiftsperiode.grunnlagInntekstperiode?.type,
            trygdeavgiftsperiode.trygdesats.toDouble(),
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi().toInt()
        )
}
