package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
import org.threeten.extra.LocalDateRange
import java.time.LocalDate

object EøsPensjonistTrygdeavgiftsberegningValidator {
    const val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    const val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"
    const val SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER = "Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt"
    const val SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE = "Skatteforholdsperiodene kan ikke overlappe"
    const val SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Skatteforholdsperioden(e) du har lagt inn dekker ikke hele helseutgift periode"
    const val INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Inntektsperioden(e) du har lagt inn dekker ikke hele helseutgift periode"
    const val INNTEKTSPERIODE_ER_UTENFOR_HELSEUTGIFT_DEKKES_PERIODE = "Inntektsperioden(e) du har lagt inn er utenfor helseutgift periode"
    const val INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNEVÆRENDE_OG_FREMTIDIG =
        "Inntektsperiode og skatteforholdsperiode må dekke helseutgiftperiode for inneværende år og fremtidige perioder"
    val log = mu.KotlinLogging.logger {}

    fun validerForTrygdeavgiftberegning(
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        behandlingsresultat: Behandlingsresultat,
        unleash: Unleash,
        dagensDato: LocalDate = LocalDate.now()
    ) {

        validerInntektsperioder(inntektsperioder, skatteforholdsperioder)
        validerSkatteforholdsperioder(skatteforholdsperioder)

        val skalValiderePerioderForNyVurderingOgManglendeInnbetaling = behandlingsresultat.hentBehandling().type in listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        ) && unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

        if (skalValiderePerioderForNyVurderingOgManglendeInnbetaling) {
            validerNyVurderingOgManglendeInnbetaling(skatteforholdsperioder, inntektsperioder, helseutgiftDekkesPeriode, dagensDato)
        } else {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = false,
                skatteforholdsperioder,
                helseutgiftDekkesPeriode,
                SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            )
        }

        if (unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING) && inntektsperioder.isNotEmpty()) {
            validerInntektsperioderErIkkeUtenforMedlemskapPeriode(
                inntektsperioder, helseutgiftDekkesPeriode
            )
        }

        val erSkattepliktigIHelePerioden = skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
        if (!erSkattepliktigIHelePerioden && !skalValiderePerioderForNyVurderingOgManglendeInnbetaling) {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = true,
                inntektsperioder,
                helseutgiftDekkesPeriode,
                INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            )
        }

    }

    private fun validerNyVurderingOgManglendeInnbetaling(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        dagensDato: LocalDate = LocalDate.now()
    ) {
        if (skatteforholdsperioder.any { it.fom.year < dagensDato.year } || inntektsperioder.any { it.fom.year < dagensDato.year }) {
            throw FunksjonellException(INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR)
        }

        val helseutgiftDekkesPeriodeIDetteOgFremtidigÅr =
            if (helseutgiftDekkesPeriode.fomDato.year < dagensDato.year) {
                HelseutgiftDekkesPeriode(
                    behandlingsresultat = helseutgiftDekkesPeriode.behandlingsresultat,
                    fomDato = LocalDate.of(dagensDato.year, 1, 1),
                    tomDato = helseutgiftDekkesPeriode.tomDato,
                    bostedLandkode = helseutgiftDekkesPeriode.bostedLandkode
                )
            } else {
                helseutgiftDekkesPeriode
            }


        validerPerioderDekkerSammenlignetPeriode(
            kanOverlappe = false,
            skatteforholdsperioder,
            helseutgiftDekkesPeriodeIDetteOgFremtidigÅr,
            INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNEVÆRENDE_OG_FREMTIDIG
        )

        validerPerioderDekkerSammenlignetPeriode(
            kanOverlappe = true,
            inntektsperioder,
            helseutgiftDekkesPeriodeIDetteOgFremtidigÅr,
            INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNEVÆRENDE_OG_FREMTIDIG
        )
    }

    private fun validerInntektsperioderErIkkeUtenforMedlemskapPeriode(
        kildeperioder: List<ErPeriode>,
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
    ) {
        val kildeperiodeStart = kildeperioder.minOf { it.fom }
        val kildeperiodeEnd = kildeperioder.maxOf { it.tom }

        val helseutgiftDekkesPeriodeStart = helseutgiftDekkesPeriode.fomDato
        val helseutgiftDekkesPeriodeSlutt = helseutgiftDekkesPeriode.tomDato

        if (kildeperiodeStart.isBefore(helseutgiftDekkesPeriodeStart)) throw FunksjonellException(
            INNTEKTSPERIODE_ER_UTENFOR_HELSEUTGIFT_DEKKES_PERIODE
        )
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
