@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
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


        harOverlapp(skatteforholdsPerioder, SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE)

        validerPerioderDekkerSammenlignetPeriode(
            skatteforholdsPerioder,
            innvilgedeMedlemskapsperioder,
            SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }
        val erSkattepliktigIHelePerioden = skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerPerioderDekkerSammenlignetPeriode(inntektsPerioder, innvilgedeMedlemskapsperioder, INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
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

    private fun validerPerioderDekkerSammenlignetPeriode(
        kildeperioder: List<ErPeriode>,
        medlemskapsperioder: List<ErPeriode>,
        feilmelding: String
    ) {
        val sorterteKildeperioder = kildeperioder.map { LocalDateRange.of(it.fom, it.tom) }.sortedBy { it.start }
        val sorterteMedlemskapsperioder = medlemskapsperioder.map { LocalDateRange.of(it.fom, it.tom) }.sortedBy { it.start }

        if (sorterteMedlemskapsperioder.isEmpty()) return

        var startDato = sorterteMedlemskapsperioder.first().start
        var kildeIndex = 0

        sorterteMedlemskapsperioder.forEach { periode ->
            while (kildeIndex < sorterteKildeperioder.size && sorterteKildeperioder[kildeIndex].start <= startDato) {
                startDato = maxOf(startDato, sorterteKildeperioder[kildeIndex].end.plusDays(1))
                kildeIndex++
            }

            if (startDato <= periode.end)
                throw FunksjonellException(feilmelding)

            startDato = maxOf(startDato, periode.start)
        }
    }

    private fun harOverlapp(perioder: List<ErPeriode>, feilmelding: String) {
        val harOverlapp = perioder
            .map { LocalDateRange.ofClosed(it.fom, it.tom) }
            .sortedBy { it.start }
            .zipWithNext()
            .any { (current, next) -> current.overlaps(next) }

        if (harOverlapp) {
            throw FunksjonellException(feilmelding)
        }
    }
}
