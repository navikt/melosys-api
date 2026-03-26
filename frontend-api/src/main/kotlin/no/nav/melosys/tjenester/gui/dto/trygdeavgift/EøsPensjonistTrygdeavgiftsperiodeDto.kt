package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Avgiftsberegningstype
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.RoundingMode
import java.time.LocalDate

data class EøsPensjonistTrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektskildetype: Inntektskildetype?,
    val avgiftssats: Double?,
    val avgiftPerMd: Int,
    val beregningstype: String? = null,
    val harSammenslåtteInntektskilder: Boolean = false,
    val avgiftsdel: String? = null
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdeavgiftsperiode.grunnlagInntekstperiode?.type,
            trygdeavgiftsperiode.trygdesats?.toDouble(),
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi().setScale(0, RoundingMode.HALF_UP).intValueExact(),
            trygdeavgiftsperiode.beregningstype.takeIf { it != Avgiftsberegningstype.ORDINAER }?.name,
            harSammenslåtteInntektskilder = trygdeavgiftsperiode.grunnlagListe
                .map { it.inntektsperiode.type }
                .distinct()
                .size > 1,
            avgiftsdel = trygdeavgiftsperiode.avgiftsdel
        )
}
