@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import org.threeten.extra.LocalDateRange

object TrygdeavgiftsberegningValidering {
    val MEDLEMSKAPSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten medlemskapsperioder"
    val UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER = "Klarte ikke finne startdatoen på medlemskapet"
    val UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER = "Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato"
    val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"
    val SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER = "Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt"

    val SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE = "Skatteforholdsperiodene kan ikke overlappe"
    val INNTEKTSPERIODENE_KAN_IKKE_OVERLAPPE = "Inntektsperiodene kan ikke overlappe"
    val SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"
    val INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"


    fun validerForTrygdeavgiftberegning(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsPerioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>
    ) {
        if (inntektsPerioder.isEmpty() && !erAllePerioderSkattepliktige(skatteforholdsPerioder)) {
            throw FunksjonellException(INNTEKTSPERIODER_EMPTY)
        }
        if (skatteforholdsPerioder.isEmpty()) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODER_EMPTY)
        }

        if (skatteforholdsPerioder.size > 1 && skatteforholdsPerioder.groupBy { it.skatteplikttype }.size == 1) {
            throw FunksjonellException(SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER)
        }

        validerMedlemskapsperioder(behandlingsresultat)

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            skatteforholdsPerioder,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }
        val erSkattepliktigIHelePerioden = skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                inntektsPerioder,
                innvilgedeMedlemskapsperioder
            )
        }
    }

    fun erAllePerioderSkattepliktige(skatteforholdsPerioder: List<SkatteforholdTilNorge>): Boolean {
        return skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
    }

    private fun validerMedlemskapsperioder(behandlingsresultat: Behandlingsresultat) {
        if (behandlingsresultat.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException(MEDLEMSKAPSPERIODER_EMPTY)
        }
        behandlingsresultat.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException(UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER)

        behandlingsresultat.utledMedlemskapsperiodeTom()
            ?: throw FunksjonellException(UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER)
    }

    private fun validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
        inntektsperioder: List<Inntektsperiode>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {

        val harOverlapp = harOverlapp(inntektsperioder)
        if (harOverlapp) {
            throw FunksjonellException(INNTEKTSPERIODENE_KAN_IKKE_OVERLAPPE)
        }

        val periodeErDekket = validerPerioderDekkerSammenlignetPeriode(inntektsperioder, innvilgedeMedlemskapsperioder)
        if (!periodeErDekket) {
            throw FunksjonellException(INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
        }
    }

    private fun validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
        skatteforholdTilNorge: List<SkatteforholdTilNorge>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {
        val harOverlapp = harOverlapp(skatteforholdTilNorge)
        if (harOverlapp) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE)
        }

        val periodeErDekket = validerPerioderDekkerSammenlignetPeriode(skatteforholdTilNorge, innvilgedeMedlemskapsperioder)
        if (!periodeErDekket) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
        }
    }


    private fun validerPerioderDekkerSammenlignetPeriode(
        kildeRanges: List<ErPeriode>,
        targetRanges: List<ErPeriode>
    ): Boolean {
        val sortedSourceRanges = kildeRanges.map { LocalDateRange.of(it.fom, it.tom) }.sortedBy { it.start }
        val sortedTargetRanges = targetRanges.map { LocalDateRange.of(it.fom, it.tom) }.sortedBy { it.start }

        if (sortedTargetRanges.isEmpty()) return true

        var currentDate = sortedTargetRanges.first().start
        var sourceIndex = 0

        for (targetRange in sortedTargetRanges) {
            while (sourceIndex < sortedSourceRanges.size && sortedSourceRanges[sourceIndex].start <= currentDate) {
                currentDate = maxOf(currentDate, sortedSourceRanges[sourceIndex].end.plusDays(1))
                sourceIndex++
            }

            if (currentDate <= targetRange.end) return false

            currentDate = maxOf(currentDate, targetRange.start)
        }

        return true
    }

    private fun harOverlapp(dateRanges: List<ErPeriode>): Boolean {
        return dateRanges
            .map { LocalDateRange.of(it.fom, it.tom) }
            .sortedBy { it.start }
            .zipWithNext()
            .any { (current, next) -> current.end >= next.start }
    }
}
