package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.MINSTEBELØP
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.ORDINÆR
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftPerAvgiftsdelDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftssatsDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.OrdinærSatsPåfølgendeÅrDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.TjuefemProsentRegelDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.ÅrsinntektDto
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Trygdeavgiften i vedtaksbrevet om frivillig medlemskap, gruppert per år.
 *
 * Et år med minst én periode etter 25 %-regelen beskrives etter 25 %-regelen. Ellers beskrives året med ordinær
 * sats hvis noen perioder har avgift, og ellers som et år med inntekt under minstebeløpet.
 */
internal class TrygdeavgiftIFrivilligBrev(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {

    private val perioderPerÅr: Map<Int, List<Trygdeavgiftsperiode>> =
        trygdeavgiftsperioder.groupBy { it.periodeFra.year }.toSortedMap()

    private val beregningsregelPerÅr: Map<Int, Avgiftsberegningsregel> =
        perioderPerÅr.mapValues { (_, perioder) -> beregningsregelForÅr(perioder) }

    val beregningsregelIÅr: Avgiftsberegningsregel? = beregningsregelPerÅr.values.firstOrNull()

    val beregningsregelNesteÅr: Avgiftsberegningsregel? = beregningsregelPerÅr.values.drop(1).firstOrNull()

    private val sisteÅrUtenOrdinærSats: Int? =
        beregningsregelPerÅr.filterValues { it != ORDINÆR }.keys.maxOrNull()

    private val påfølgendeÅrMedOrdinærSats: Int? = sisteÅrUtenOrdinærSats?.let { sisteÅr ->
        beregningsregelPerÅr.filter { (år, regel) -> år > sisteÅr && regel == ORDINÆR }.keys.minOrNull()
    }

    private val årMedOrdinærSatsFørSisteÅrUtenOrdinærSats: List<Int> =
        beregningsregelPerÅr.filter { (år, regel) -> regel == ORDINÆR && (sisteÅrUtenOrdinærSats == null || år < sisteÅrUtenOrdinærSats) }
            .keys.toList()

    private val årMedTjuefemProsentRegel: List<Int> =
        beregningsregelPerÅr.filterValues { it == TJUEFEM_PROSENT_REGEL }.keys.toList()

    val visOrdinærBeregningstabell: Boolean =
        årMedTjuefemProsentRegel.isEmpty() && årMedOrdinærSatsFørSisteÅrUtenOrdinærSats.isNotEmpty()

    val tjuefemProsentRegel: TjuefemProsentRegelDto? = årMedTjuefemProsentRegel.takeIf { it.isNotEmpty() }?.let {
        TjuefemProsentRegelDto(
            avgiftssatser = (årMedTjuefemProsentRegel + årMedOrdinærSatsFørSisteÅrUtenOrdinærSats).sorted()
                .flatMap { år -> avgiftssatser(år) },
            avgiftPerAvgiftsdel = årMedTjuefemProsentRegel.flatMap { år -> avgiftPerAvgiftsdel(år) },
        )
    }

    val årsinntektUnderMinstebeløp: List<ÅrsinntektDto> = perioderPerÅr.mapNotNull { (år, perioder) ->
        perioder.filter { it.beregningsregel == MINSTEBELØP }
            .takeIf { it.isNotEmpty() }
            ?.let { ÅrsinntektDto(år, it.sumOf { periode -> periode.avgiftspliktigÅrsinntekt() }.setScale(0, RoundingMode.HALF_UP)) }
    }

    val ordinærSatsPåfølgendeÅr: OrdinærSatsPåfølgendeÅrDto? = påfølgendeÅrMedOrdinærSats?.let { år ->
        OrdinærSatsPåfølgendeÅrDto(etterÅr = sisteÅrUtenOrdinærSats!!, år = år, avgiftssatser = avgiftssatser(år))
    }

    private fun avgiftssatser(år: Int): List<AvgiftssatsDto> =
        perioderPerÅr.getValue(år)
            .filter { it.beregningsregel == TJUEFEM_PROSENT_REGEL || (it.beregningsregel == ORDINÆR && it.harAvgift()) }
            .map { periode ->
                val erTjuefemProsentRegel = periode.beregningsregel == TJUEFEM_PROSENT_REGEL
                val inntektskildetyper = periode.inntektskildetyper()
                AvgiftssatsDto(
                    år = år,
                    avgiftssats = periode.trygdesats.takeUnless { erTjuefemProsentRegel },
                    beregningsregel = periode.beregningsregel,
                    inntektskildetype = inntektskildetyper.singleOrNull(),
                    harSammenslåtteInntektskilder = inntektskildetyper.size > 1,
                    trygdedekning = periode.hentGrunnlagMedlemskapsperiode().hentTrygdedekning().name,
                    avgiftsdel = periode.avgiftsdel,
                )
            }
            .distinct()
            .sortedWith(compareBy({ it.avgiftsdel?.ordinal ?: -1 }, { it.beregningsregel == TJUEFEM_PROSENT_REGEL }))

    private fun Trygdeavgiftsperiode.inntektskildetyper(): Set<Inntektskildetype> =
        grunnlagListe.map { it.inntektsperiode.type }.toSet().ifEmpty { setOf(hentGrunnlagInntekstperiode().type) }

    private fun avgiftPerAvgiftsdel(år: Int): List<AvgiftPerAvgiftsdelDto> {
        val perioder = perioderPerÅr.getValue(år)
        val tjuefemProsentRegel = perioder.filter { it.beregningsregel == TJUEFEM_PROSENT_REGEL }.map {
            AvgiftPerAvgiftsdelDto(år, it.avgiftsdel, TJUEFEM_PROSENT_REGEL, it.trygdeavgiftsbeløpMd.hentVerdi().setScale(0, RoundingMode.DOWN))
        }
        val ordinærSats = perioder.filter { it.beregningsregel == ORDINÆR && it.harAvgift() }
            .groupBy { it.avgiftsdel }
            .map { (avgiftsdel, perioderForDel) ->
                AvgiftPerAvgiftsdelDto(år, avgiftsdel, ORDINÆR, gjennomsnittligAvgiftPerMd(perioderForDel))
            }
        return tjuefemProsentRegel.sortedBy { it.avgiftsdel?.ordinal ?: -1 } + ordinærSats.sortedBy { it.avgiftsdel?.ordinal ?: -1 }
    }

    private fun gjennomsnittligAvgiftPerMd(perioder: List<Trygdeavgiftsperiode>): BigDecimal {
        val totalavgift = TotalbeløpBeregner.hentTotalavgift(perioder) ?: BigDecimal.ZERO
        return TotalbeløpBeregner.månedligBeløpForTotalbeløp(
            perioder.minOf { it.periodeFra },
            perioder.maxOf { it.periodeTil },
            totalavgift
        ).setScale(0, RoundingMode.DOWN)
    }

    private fun Trygdeavgiftsperiode.avgiftspliktigÅrsinntekt(): BigDecimal {
        if (grunnlagListe.isEmpty()) return TotalbeløpBeregner.hentTotalinntekt(listOf(this))

        return grunnlagListe
            .distinctBy { Triple(it.inntektsperiode, it.skatteforhold, it.hentAvgiftspliktigperiode()) }
            .filter { it.inngårIAvgiftspliktigÅrsinntekt() }
            .sumOf { grunnlag ->
                val fom = maxOf(periodeFra, grunnlag.inntektsperiode.fom, grunnlag.skatteforhold.fom, grunnlag.hentAvgiftspliktigperiode().fom)
                val tom = minOf(
                    periodeTil,
                    grunnlag.inntektsperiode.tom ?: LocalDate.MAX,
                    grunnlag.skatteforhold.tom ?: LocalDate.MAX,
                    grunnlag.hentAvgiftspliktigperiode().tom ?: LocalDate.MAX
                )
                if (fom.isAfter(tom)) BigDecimal.ZERO
                else with(TotalbeløpBeregner) { totalbeløpForPeriode(fom, tom, grunnlag.inntektsperiode.kalkulertMndInntekt()) }
            }
    }

    /**
     * Samme avgrensning som beregningskomponenten bruker for minstebeløpet ved frivillig medlemskap
     * (MaksimalAvgift.skalInkluderesFrivillig i melosys-trygdeavgift-beregning): inntekt der ordinær trygdeavgift
     * og arbeidsgiveravgift betales til Skatteetaten, holdes utenfor.
     */
    private fun TrygdeavgiftsperiodeGrunnlag.inngårIAvgiftspliktigÅrsinntekt(): Boolean {
        val inntektskildetype = inntektsperiode.type
        return when {
            inntektskildetype in INNTEKTSKILDETYPER_SOM_ALLTID_INNGÅR -> true
            skatteforhold.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG -> true
            inntektskildetype == Inntektskildetype.MISJONÆR -> false
            else -> !inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt
        }
    }

    companion object {
        fun harUlikBeregningsregelPerÅr(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): Boolean =
            trygdeavgiftsperioder.groupBy { it.periodeFra.year }.values.map(::beregningsregelForÅr).distinct().size > 1

        private fun beregningsregelForÅr(perioder: List<Trygdeavgiftsperiode>): Avgiftsberegningsregel = when {
            perioder.any { it.beregningsregel == TJUEFEM_PROSENT_REGEL } -> TJUEFEM_PROSENT_REGEL
            perioder.any { it.beregningsregel == ORDINÆR && it.harAvgift() } -> ORDINÆR
            perioder.any { it.beregningsregel == MINSTEBELØP } -> MINSTEBELØP
            else -> ORDINÆR
        }

        private val INNTEKTSKILDETYPER_SOM_ALLTID_INNGÅR = setOf(
            Inntektskildetype.FN_SKATTEFRITAK,
            Inntektskildetype.PENSJON_UFØRETRYGD,
            Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT,
            Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE,
        )
    }
}
