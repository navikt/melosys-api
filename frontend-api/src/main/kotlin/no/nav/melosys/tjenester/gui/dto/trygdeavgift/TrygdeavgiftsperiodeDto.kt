package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Avgiftsdel
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
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
    val avgiftsdel: Avgiftsdel? = null
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdedekning = trygdeavgiftsperiode.hentGrunnlagAvgiftsperiode().hentTrygdedekning(),
            trygdeavgiftsperiode.grunnlagInntekstperiode?.type,
            trygdeavgiftsperiode.trygdesats?.toDouble(),
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi().intValueExact(),
            trygdeavgiftsperiode.beregningsregel.takeIf { it != Avgiftsberegningsregel.ORDINÆR }?.name,
            harSammenslåtteInntektskilder = trygdeavgiftsperiode.grunnlagListe
                .map { it.inntektsperiode.type }
                .distinct()
                .size > 1,
            avgiftsdel = trygdeavgiftsperiode.avgiftsdel
        )
}
