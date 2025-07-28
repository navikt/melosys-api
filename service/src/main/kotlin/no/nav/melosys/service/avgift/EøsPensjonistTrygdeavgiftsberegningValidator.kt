package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import org.threeten.extra.LocalDateRange
import kotlin.collections.component1
import kotlin.collections.component2
object EøsPensjonistTrygdeavgiftsberegningValidator {
    const val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    const val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"
    const val SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER = "Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt"
    const val SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE = "Skatteforholdsperiodene kan ikke overlappe"
    const val SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Skatteforholdsperioden(e) du har lagt inn dekker ikke hele helseutgift dekkes periode"
    const val INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Inntektsperioden(e) du har lagt inn dekker ikke hele helseutgift dekkes periode"
    const val INNTEKTSPERIODE_ER_UTENFOR_HELSEUTGIFT_DEKKES_PERIODE = "Inntektsperioden(e) du har lagt inn er utenfor helseutgift dekkes periode"
    val log = mu.KotlinLogging.logger {}

    fun validerForTrygdeavgiftberegning(
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        unleash: Unleash
    ) {

        validerInntektsperioder(inntektsperioder, skatteforholdsperioder)
        validerSkatteforholdsperioder(skatteforholdsperioder)
        validerPerioderDekkerSammenlignetPeriode(
            kanOverlappe = false,
            skatteforholdsperioder,
            helseutgiftDekkesPeriode,
            SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
        )

        if (unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING) && inntektsperioder.isNotEmpty()) {
            validerInntektsperioderErIkkeUtenforMedlemskapPeriode(
                inntektsperioder, helseutgiftDekkesPeriode
            )
        }

        val erSkattepliktigIHelePerioden = skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
        if (!erSkattepliktigIHelePerioden) {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = true,
                inntektsperioder,
                helseutgiftDekkesPeriode,
                INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            )
        }

    }

    private fun validerInntektsperioderErIkkeUtenforMedlemskapPeriode(
        kildeperioder: List<ErPeriode>,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
    ) {
        val kildeperiodeStart = kildeperioder.minOf { it.fom }
        val kildeperiodeEnd = kildeperioder.maxOf { it.tom }

        val helseutgiftDekkesPeriodeStart = helseutgiftDekkesPeriode.fomDato
        val helseutgiftDekkesPeriodeSlutt = helseutgiftDekkesPeriode.tomDato

        if (kildeperiodeStart.isBefore(helseutgiftDekkesPeriodeStart)) throw FunksjonellException(INNTEKTSPERIODE_ER_UTENFOR_HELSEUTGIFT_DEKKES_PERIODE)
        if (kildeperiodeEnd.isAfter(helseutgiftDekkesPeriodeSlutt)) throw FunksjonellException(INNTEKTSPERIODE_ER_UTENFOR_HELSEUTGIFT_DEKKES_PERIODE)
    }

    fun erAllePerioderSkattepliktige(skatteforholdsPerioder: List<SkatteforholdTilNorge>): Boolean {
        return skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
    }

    private fun validerPerioderDekkerSammenlignetPeriode(
        kanOverlappe: Boolean,
        kildeperioder: List<ErPeriode>,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        feilmelding: String
    ) {
        val kildeperiodeStart = kildeperioder.minOf { it.fom }
        val kildeperiodeEnd = kildeperioder.maxOf { it.tom }

        val helseutgiftDekkesPeriodeStart = helseutgiftDekkesPeriode.fomDato
        val helseutgiftDekkesPeriodeSlutt = helseutgiftDekkesPeriode.tomDato

        if (!(kildeperiodeStart.isEqual(helseutgiftDekkesPeriodeStart) && kildeperiodeEnd.isEqual(helseutgiftDekkesPeriodeSlutt))) {
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

    private fun validerInntektsperioder(
        inntektsperioder: List<Inntektsperiode>,
        skatteforholdsperioder: List<SkatteforholdTilNorge>
    ) {
        if (inntektsperioder.isEmpty() && !erAllePerioderSkattepliktige(skatteforholdsperioder)) {
            throw FunksjonellException(INNTEKTSPERIODER_EMPTY)
        }
    }



    private fun validerSkatteforholdsperioder(skatteforholdsperioder: List<SkatteforholdTilNorge>) {
        if (skatteforholdsperioder.isEmpty()) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODER_EMPTY)
        }

        if (skatteforholdsperioder.size > 1 && skatteforholdsperioder.groupBy { it.skatteplikttype }.size == 1) {
            throw FunksjonellException(SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER)
        }

        harOverlapp(skatteforholdsperioder)

    }

    private fun harOverlapp(perioder: List<ErPeriode>) {
        val harOverlapp = perioder
            .map { LocalDateRange.ofClosed(it.fom, it.tom) }
            .sortedBy { it.start }
            .zipWithNext()
            .any { (current, next) -> current.overlaps(next) }

        if (harOverlapp) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE)
        }
    }
}
