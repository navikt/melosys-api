@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.SkatteforholdsperiodeDto
import org.threeten.extra.LocalDateRange
import java.time.DateTimeException

object TrygdeavgiftValideringService {
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
        skatteforholdsPerioder: List<SkatteforholdsperiodeDto>,
        inntektsPerioder: List<InntektsperiodeDto>
    ) {
        if (inntektsPerioder.isEmpty() && !erAllePerioderSkattepliktige(skatteforholdsPerioder)) {
            throw FunksjonellException(INNTEKTSPERIODER_EMPTY)
        }
        if (skatteforholdsPerioder.isEmpty()) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODER_EMPTY)
        }

        if (skatteforholdsPerioder.size > 1 && skatteforholdsPerioder.groupBy { it.skatteforhold }.size == 1) {
            throw FunksjonellException(SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER)
        }

        validerMedlemskapsperioder(behandlingsresultat)

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkkeNew(
            skatteforholdsPerioder,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }
        val erSkattepliktigIHelePerioden = skatteforholdsPerioder.all { it.skatteforhold == Skatteplikttype.SKATTEPLIKTIG }
        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioderNew(
                inntektsPerioder,
                innvilgedeMedlemskapsperioder
            )
        }
    }

    fun erAllePerioderSkattepliktige(skatteforholdsPerioder: List<SkatteforholdsperiodeDto>): Boolean {
        return skatteforholdsPerioder.all { it.skatteforhold == Skatteplikttype.SKATTEPLIKTIG }
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

    private fun validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioderNew(
        inntektsperioder: List<InntektsperiodeDto>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {
        val inntektsperiodeDateRange = inntektsperioder.sortedBy { it.periode.fom }
            .map { inntektsperiode -> LocalDateRange.ofClosed(inntektsperiode.periode.fom, inntektsperiode.periode.tom) }

        var samletInntektsperiodeDateRange = finnRangeForPerioderSamlet(inntektsperiodeDateRange, INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
        if (validerPeriodeDekkerIkkeHeleMedlemskapsPerioden(innvilgedeMedlemskapsperioder, samletInntektsperiodeDateRange)) {
            throw FunksjonellException(INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
        }
    }

    private fun validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkkeNew(
        skatteforholdTilNorge: List<SkatteforholdsperiodeDto>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {
        val skatteforholdDateRange = skatteforholdTilNorge.sortedBy { it.periode.fom }
            .map { skatteforhold -> LocalDateRange.ofClosed(skatteforhold.periode.fom, skatteforhold.periode.tom) }

        validerAtDetIkkeFinnesOverlapp(skatteforholdDateRange)

        var samletSkatteforholdDateRange = finnRangeForPerioderSamlet(skatteforholdDateRange, SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN)

        if (validerPeriodeDekkerIkkeHeleMedlemskapsPerioden(innvilgedeMedlemskapsperioder, samletSkatteforholdDateRange)) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN)
        }
    }

    private fun finnRangeForPerioderSamlet(periodeRanges: List<LocalDateRange>, feilmelding: String): LocalDateRange? {
        var samletPeriodeDateRange: LocalDateRange? = null
        try {
            for (range in periodeRanges) {
                samletPeriodeDateRange = if (samletPeriodeDateRange == null) {
                    range
                } else {
                    samletPeriodeDateRange.union(range)
                }
            }
        } catch (ex: DateTimeException) {
            throw FunksjonellException(feilmelding)
        }

        return samletPeriodeDateRange
    }

    private fun validerPeriodeDekkerIkkeHeleMedlemskapsPerioden(
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>, periodeDateRange: LocalDateRange?
    ): Boolean {
        val sortertMedlemskapsperiode = innvilgedeMedlemskapsperioder.sortedBy { it.fom }
        return LocalDateRange.ofClosed(
            sortertMedlemskapsperiode.first().fom,
            sortertMedlemskapsperiode.last().tom
        ) != periodeDateRange
    }

    private fun validerAtDetIkkeFinnesOverlapp(dateRanges: List<LocalDateRange>) {
        for (i in dateRanges.indices) {
            val range1 = dateRanges[i]

            for (j in dateRanges.indices) {
                if (i != j) {
                    val range2 = dateRanges[j]

                    if (range1.overlaps(range2)) {
                        throw FunksjonellException(SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE)
                    }
                }
            }
        }
    }
}
