@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.SkatteforholdsperiodeDto
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import org.threeten.extra.LocalDateRange
import java.time.DateTimeException

object TrygdeavgiftValideringService {
    val MEDLEMSKAPSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten medlemskapsperioder"
    val UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER = "Klarte ikke finne startdatoen på medlemskapet"
    val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"


    fun validerTrygdeavgiftberegningRequest(request: OppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat: Behandlingsresultat) {
        validerMedlemskapsperioder(behandlingsresultat)
        validerTrygdeavgiftsgrunnlag(request, behandlingsresultat)
    }

    fun erAllePerioderSkattepliktige(request: OppdaterTrygdeavgiftsgrunnlagRequest): Boolean {
        return request.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
    }

    private fun validerMedlemskapsperioder(behandlingsresultat: Behandlingsresultat) {
        if (behandlingsresultat.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException(MEDLEMSKAPSPERIODER_EMPTY)
        }
        behandlingsresultat.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException(UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER)
    }

    fun validerForTrygdeavgiftberegning(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsPerioder: List<SkatteforholdsperiodeDto>,
        inntektsPerioder: List<InntektsperiodeDto>
    ) {
        validerMedlemskapsperioder(behandlingsresultat)
        if (inntektsPerioder.isEmpty() && !erAllePerioderSkattepliktige(skatteforholdsPerioder)) {
            throw FunksjonellException(INNTEKTSPERIODER_EMPTY)
        }
        if (skatteforholdsPerioder.isEmpty()) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODER_EMPTY)
        }
    }

    fun erAllePerioderSkattepliktige(skatteforholdsPerioder: List<SkatteforholdsperiodeDto>): Boolean {
        return skatteforholdsPerioder.all { it.skatteforhold == Skatteplikttype.SKATTEPLIKTIG }
    }

    private fun validerTrygdeavgiftsgrunnlag(request: OppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat: Behandlingsresultat) {
        if (request.inntektskilder.isEmpty() && !erAllePerioderSkattepliktige(request)) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten inntektsperioder")
        } //

        if (request.skatteforholdTilNorgeList.isEmpty()) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
        } //

        val erSkattepliktigIHelePerioden = request.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        val medlemskapsperioderErÅpen = behandlingsresultat.utledMedlemskapsperiodeTom() == null
        if (medlemskapsperioderErÅpen) {
            throw FunksjonellException("Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato")
        }

        if (request.skatteforholdTilNorgeList.size > 1 && request.skatteforholdTilNorgeList.groupBy { it.skatteplikttype }.size == 1) {
            throw FunksjonellException("Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt")
        }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            request.skatteforholdTilNorgeList,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }

        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                request.inntektskilder,
                innvilgedeMedlemskapsperioder
            )
        }
    }

    /*
    private fun validerTrygdeavgiftsgrunnlag(request: OppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat: Behandlingsresultat) {
        if (request.inntektskilder.isEmpty() && !erAllePerioderSkattepliktige(request)) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten inntektsperioder")
        }
        if (request.skatteforholdTilNorgeList.isEmpty()) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
        }

        val erSkattepliktigIHelePerioden = request.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        val medlemskapsperioderErÅpen = behandlingsresultat.utledMedlemskapsperiodeTom() == null
        if (medlemskapsperioderErÅpen) {
            throw FunksjonellException("Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato")
        }

        if (request.skatteforholdTilNorgeList.size > 1 && request.skatteforholdTilNorgeList.groupBy { it.skatteplikttype }.size == 1) {
            throw FunksjonellException("Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt")
        }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            request.skatteforholdTilNorgeList,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }

        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                request.inntektskilder,
                innvilgedeMedlemskapsperioder
            )
        }
    }

     */


    private fun validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
        inntektsperioder: List<InntektskildeRequest>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {
        val inntektsperiodeDateRange = inntektsperioder.sortedBy { it.fomDato }
            .map { inntektsperiode -> LocalDateRange.ofClosed(inntektsperiode.fomDato, inntektsperiode.tomDato) }

        var samletInntektsperiodeDateRange: LocalDateRange? = null
        try {
            for (range in inntektsperiodeDateRange) {
                samletInntektsperiodeDateRange = if (samletInntektsperiodeDateRange == null) {
                    range
                } else {
                    samletInntektsperiodeDateRange.union(range)
                }
            }
        } catch (ex: DateTimeException) {
            throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
        }

        val sortertMedlemskapsperiode = innvilgedeMedlemskapsperioder.sortedBy { it.fom }
        if (LocalDateRange.ofClosed(
                sortertMedlemskapsperiode.first().fom,
                sortertMedlemskapsperiode.last().tom
            ) != samletInntektsperiodeDateRange
        ) {
            throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
        }
    }


    private fun validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
        skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
    ) {
        val skatteforholdDateRange = skatteforholdTilNorge.sortedBy { it.fomDato }
            .map { skatteforhold -> LocalDateRange.ofClosed(skatteforhold.fomDato, skatteforhold.tomDato) }

        validerAtDetIkkeFinnesOverlapp(skatteforholdDateRange)

        var samletSkatteforholdDateRange: LocalDateRange? = null
        try {
            for (range in skatteforholdDateRange) {
                samletSkatteforholdDateRange = if (samletSkatteforholdDateRange == null) {
                    range
                } else {
                    samletSkatteforholdDateRange.union(range)
                }
            }
        } catch (ex: DateTimeException) {
            throw FunksjonellException("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
        }

        val sortertMedlemskapsperiode = innvilgedeMedlemskapsperioder.sortedBy { it.fom }
        if (LocalDateRange.ofClosed(
                sortertMedlemskapsperiode.first().fom,
                sortertMedlemskapsperiode.last().tom
            ) != samletSkatteforholdDateRange
        ) {
            throw FunksjonellException("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
        }
    }

    private fun validerAtDetIkkeFinnesOverlapp(dateRanges: List<LocalDateRange>) {
        for (i in dateRanges.indices) {
            val range1 = dateRanges[i]

            for (j in dateRanges.indices) {
                if (i != j) {
                    val range2 = dateRanges[j]

                    if (range1.overlaps(range2)) {
                        throw FunksjonellException("Skatteforholdsperiodene kan ikke overlappe")
                    }
                }
            }
        }
    }
}
