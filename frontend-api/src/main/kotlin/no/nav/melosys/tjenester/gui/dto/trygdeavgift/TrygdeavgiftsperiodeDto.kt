package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Avgiftsdel
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.SammenslåttGrunnlagBeregner
import java.math.BigDecimal
import java.time.LocalDate

data class TrygdeavgiftsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val inntektskildetype: Inntektskildetype?,
    val inntektPerMd: BigDecimal,
    /** Null når de sammenslåtte inntektene ikke er enige — se [SammenslåttGrunnlagBeregner]. */
    val arbeidsgiversavgiftBetales: Boolean?,
    val avgiftssats: Double?,
    val avgiftPerMd: Int,
    val beregningsregel: Avgiftsberegningsregel,
    val harSammenslåtteInntektskilder: Boolean = false,
    val avgiftsdel: Avgiftsdel? = null
) {
    constructor(trygdeavgiftsperiode: Trygdeavgiftsperiode) :
        this(
            trygdeavgiftsperiode.periodeFra,
            trygdeavgiftsperiode.periodeTil,
            trygdedekning = trygdeavgiftsperiode.hentGrunnlagAvgiftsperiode().hentTrygdedekning(),
            trygdeavgiftsperiode.grunnlagInntekstperiode?.type,
            inntektPerMd = SammenslåttGrunnlagBeregner.bruttoinntektPerMd(trygdeavgiftsperiode, verdiAvrundet = true),
            arbeidsgiversavgiftBetales = SammenslåttGrunnlagBeregner.arbeidsgiversavgiftBetales(trygdeavgiftsperiode),
            trygdeavgiftsperiode.trygdesats?.toDouble(),
            trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi().intValueExact(),
            trygdeavgiftsperiode.beregningsregel,
            harSammenslåtteInntektskilder = trygdeavgiftsperiode.harSammenslåtteInntektskilder,
            avgiftsdel = trygdeavgiftsperiode.avgiftsdel
        )
}
