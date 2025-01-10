@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import org.threeten.extra.LocalDateRange

object TrygdeavgiftsberegningValidator {
    const val MEDLEMSKAPSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten medlemskapsperioder"
    const val UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER = "Klarte ikke finne startdatoen på medlemskapet"
    const val UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER = "Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato"
    const val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    const val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"
    const val SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER = "Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt"
    const val SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE = "Skatteforholdsperiodene kan ikke overlappe"
    const val SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"
    const val INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"


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
            kanOverlappe = false,
            skatteforholdsPerioder,
            innvilgedeMedlemskapsperioder,
            SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }
        val erSkattepliktigIHelePerioden = skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = true,
                inntektsPerioder,
                innvilgedeMedlemskapsperioder,
                INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
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

    private fun validerPerioderDekkerSammenlignetPeriode(
        kanOverlappe: Boolean,
        kildeperioder: List<ErPeriode>,
        medlemskapsperioder: List<ErPeriode>,
        feilmelding: String
    ) {
        val kildeperiodeStart = kildeperioder.minOf { it.fom }
        val kildeperiodeEnd = kildeperioder.maxOf { it.tom }

        val medlemskapsPeriodestart = medlemskapsperioder.minOf { it.fom }
        val medlemskapsPeriodeEnd = medlemskapsperioder.maxOf { it.tom }

        if (!(kildeperiodeStart.isEqual(medlemskapsPeriodestart) && kildeperiodeEnd.isEqual(medlemskapsPeriodeEnd))) {
            throw FunksjonellException(feilmelding)
        }

        val sorterteKildeperioder = kildeperioder.map { LocalDateRange.of(it.fom, it.tom) }.sortedBy { it.start }
        sorterteKildeperioder.windowed(2).forEach { (current, next) ->
            if (kanOverlappe && current.end.plusDays(1) < next.start) {
                throw FunksjonellException(feilmelding)
            }

            if (!kanOverlappe && current.end.plusDays(1) != next.start) {
                throw FunksjonellException(feilmelding)
            }

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
