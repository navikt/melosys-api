package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.math.RoundingMode
import java.time.LocalDate

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val inntektskildetype: Inntektskildetype?,
    val avgiftssats: Double?,
    val avgiftPerMd: Int,
    val beregningsregel: String? = null,
    val harSammenslåtteInntektskilder: Boolean = false,
    val avgiftsdel: String? = null
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdedekning = trygdeavgiftsperiode.hentGrunnlagAvgiftsperiode().hentTrygdedekning(),
            trygdeavgiftsperiode.grunnlagInntekstperiode?.type,
            trygdeavgiftsperiode.trygdesats?.toDouble(),
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi().setScale(0, RoundingMode.HALF_UP).intValueExact(),
            trygdeavgiftsperiode.beregningsregel.takeIf { it != Avgiftsberegningsregel.ORDINÆR }?.name,
            harSammenslåtteInntektskilder = trygdeavgiftsperiode.grunnlagListe
                .map { it.inntektsperiode.type }
                .distinct()
                .size > 1,
            avgiftsdel = trygdeavgiftsperiode.avgiftsdel
        )
}
