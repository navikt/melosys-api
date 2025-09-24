@file:Suppress("LocalVariableName")

package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Inntektskildetype.*
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.PENSJONIST
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import org.threeten.extra.LocalDateRange
import java.time.LocalDate


object TrygdeavgiftsberegningValidator {
    const val BEHANDLING_IKKE_AKTIV = "Kan ikke beregne trygdeavgift på avsluttet behandling"
    const val MEDLEMSKAPSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten medlemskapsperioder"
    const val UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER = "Det kreves en innvilget medlemskapsperiode med startdato"
    const val UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER =
        "Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato"
    const val MEDLEMSKAPSPERIODER_HAR_FORSKJELLIGE_BESTEMMELSER = "Det er ikke lov med forskjellige bestemmelser"

    const val INNTEKTSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten inntektsperioder"
    const val SKATTEFORHOLDSPERIODER_EMPTY = "Kan ikke beregne trygdeavgift uten skatteforholdTilNorge"
    const val SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER = "Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt"
    const val SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE = "Skatteforholdsperiodene kan ikke overlappe"
    const val SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"
    const val INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN = "Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)"
    const val INNTEKTSPERIODE_ER_UTENFOR_MEDLEMSKAPSPERIODE = "Inntektsperioden(e) du har lagt inn er utenfor medlemskapsperioden(e)"
    const val MINST_EN_ANNEN_INNTEKT_I_TILLEGG_TIL_PENSJON = "Du må oppgi minst en annen inntekt i tillegg til pensjon/uføretrygd"
    const val MEDLEMSKAPSPERIODER_HAR_OPPHOLD = "Medlemskapsperiodene kan ikke ha opphold."
    const val SKATTEPLIKTIG_OG_PENSJON_UFORETRYGD_MED_KILDESKATT =
        "Inntekstypen \"Pensjon/uføretrygd det betales kildeskatt av\"; kan ikke velges for perioder bruker er skattepliktig til Norge."
    const val INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG =
        "Inntektsperiode og skatteforholdsperiode må dekke medlemskapsperioden for inneværende år og fremtidige perioder"
    const val INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR = "Inntektsperiode eller skatteforholdsperiode kan ikke være i tidligere år"

    fun validerForTrygdeavgiftberegning(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        unleash: Unleash,
        dagensDato: LocalDate = LocalDate.now(),
    ) {
        if (behandlingsresultat.behandling.erInaktiv()) {
            throw FunksjonellException(BEHANDLING_IKKE_AKTIV)
        }

        if (inntektsperioder.isEmpty() && !erAllePerioderSkattepliktige(skatteforholdsperioder)) {
            throw FunksjonellException(INNTEKTSPERIODER_EMPTY)
        }

        if (inntektsperioder.isNotEmpty() && behandlingsresultat.behandling.tema != PENSJONIST
            && inntektsperioder.all { listOf(PENSJON_UFØRETRYGD, PENSJON_UFØRETRYGD_KILDESKATT, PENSJON, UFØRETRYGD).contains(it.type) }
        ) {
            throw FunksjonellException(MINST_EN_ANNEN_INNTEKT_I_TILLEGG_TIL_PENSJON)
        }

        if (skatteforholdsperioder.isEmpty()) {
            throw FunksjonellException(SKATTEFORHOLDSPERIODER_EMPTY)
        }

        if (skatteforholdsperioder.size > 1 && skatteforholdsperioder.groupBy { it.skatteplikttype }.size == 1) {
            throw FunksjonellException(SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER)
        }

        if (erSkattepliktigOgPensjonUføreMedKildeskatt(skatteforholdsperioder, inntektsperioder))
            throw FunksjonellException(SKATTEPLIKTIG_OG_PENSJON_UFORETRYGD_MED_KILDESKATT)

        validerMedlemskapsperioder(behandlingsresultat, unleash)

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        harOverlapp(skatteforholdsperioder, SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE)

        val skalValiderePerioderForNyVurderingOgManglendeInnbetaling = behandlingsresultat.behandling.type in listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        ) && unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

        if (skalValiderePerioderForNyVurderingOgManglendeInnbetaling) {
            validerNyVurderingOgManglendeInnbetaling(
                skatteforholdsperioder,
                inntektsperioder,
                innvilgedeMedlemskapsperioder,
                dagensDato
            )
        } else {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = false,
                skatteforholdsperioder,
                innvilgedeMedlemskapsperioder,
                SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            )
        }

        if (unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING) && inntektsperioder.isNotEmpty()) {
            validerinntektsperioderErIkkeUtenforMedlemskapPeriode(
                inntektsperioder, innvilgedeMedlemskapsperioder, INNTEKTSPERIODE_ER_UTENFOR_MEDLEMSKAPSPERIODE
            )
        }

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }
        val erSkattepliktigIHelePerioden = skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden) && !skalValiderePerioderForNyVurderingOgManglendeInnbetaling) {
            validerPerioderDekkerSammenlignetPeriode(
                kanOverlappe = true,
                inntektsperioder,
                innvilgedeMedlemskapsperioder,
                INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            )
        }
    }

    private fun validerNyVurderingOgManglendeInnbetaling(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ) {
        if (skatteforholdsperioder.any { it.fom.year < dagensDato.year } || inntektsperioder.any { it.fom.year < dagensDato.year }) {
            throw FunksjonellException(INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR)
        }

        val medlemskapsperioderIDetteOgFremtidigeÅr = innvilgedeMedlemskapsperioder.map { periode ->
            if (periode.fom.year < dagensDato.year) {
                object : ErPeriode {
                    override fun getFom(): LocalDate = dagensDato.withDayOfYear(1)
                    override fun getTom(): LocalDate? = periode.tom
                }
            } else {
                periode
            }
        }
        validerPerioderDekkerSammenlignetPeriode(
            kanOverlappe = false,
            skatteforholdsperioder,
            medlemskapsperioderIDetteOgFremtidigeÅr,
            INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
        )

        validerPerioderDekkerSammenlignetPeriode(
            kanOverlappe = true,
            inntektsperioder,
            medlemskapsperioderIDetteOgFremtidigeÅr,
            INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
        )
    }

    private fun erSkattepliktigOgPensjonUføreMedKildeskatt(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektskilder: List<Inntektsperiode>
    ): Boolean {
        val skattepliktigePerioder = skatteforholdsperioder.filter { skatteforhold ->
            skatteforhold.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG
        }

        val inntektskilderPensjonUføreMedKildeskatt = inntektskilder.filter { inntektskilde ->
            inntektskilde.type == PENSJON_UFØRETRYGD_KILDESKATT
        }

        return skattepliktigePerioder.any { skatteforhold ->
            inntektskilderPensjonUføreMedKildeskatt.any { kilder ->
                PeriodeRegler.periodeOverlapper(kilder, skatteforhold)
            }
        }
    }

    private fun validerinntektsperioderErIkkeUtenforMedlemskapPeriode(
        kildeperioder: List<ErPeriode>,
        medlemskapsperioder: List<ErPeriode>,
        feilmelding: String
    ) {
        val kildeperiodeStart = kildeperioder.minOf { it.fom }
        val kildeperiodeEnd = kildeperioder.maxOf { it.tom }

        val medlemskapsPeriodestart = medlemskapsperioder.minOf { it.fom }
        val medlemskapsPeriodeEnd = medlemskapsperioder.maxOf { it.tom }

        if (kildeperiodeStart.isBefore(medlemskapsPeriodestart)) throw FunksjonellException(feilmelding)
        if (kildeperiodeEnd.isAfter(medlemskapsPeriodeEnd)) throw FunksjonellException(feilmelding)
    }

    fun erAllePerioderSkattepliktige(skatteforholdsPerioder: List<SkatteforholdTilNorge>): Boolean {
        return skatteforholdsPerioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
    }

    private fun validerMedlemskapsperioder(behandlingsresultat: Behandlingsresultat, unleash: Unleash) {
        if (behandlingsresultat.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException(MEDLEMSKAPSPERIODER_EMPTY)
        }
        if (unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING) && behandlingsresultat.årsavregning != null) {
            alleMedlemskapsperioderHarSammeBestemmelse(behandlingsresultat.medlemskapsperioder)
            medlemskapsperioderHarOpphold(behandlingsresultat.medlemskapsperioder)
        }

        behandlingsresultat.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException(UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER)

        behandlingsresultat.utledMedlemskapsperiodeTom()
            ?: throw FunksjonellException(UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER)
    }

    private fun medlemskapsperioderHarOpphold(medlemskapsperioder: Collection<Medlemskapsperiode>) {
        val medlemskapsperioderSortertPåDato = medlemskapsperioder.sortedBy { it.fom }

        medlemskapsperioderSortertPåDato.zipWithNext().forEach { (forrigePeriodeTom, nestePeriodeFom) ->
            if (forrigePeriodeTom.tom.plusDays(1).isBefore(nestePeriodeFom.fom)) {
                throw FunksjonellException(
                    MEDLEMSKAPSPERIODER_HAR_OPPHOLD
                )
            }
        }
    }

    private fun alleMedlemskapsperioderHarSammeBestemmelse(medlemskapsperioder: Collection<Medlemskapsperiode>) {
        val bestemmelse = medlemskapsperioder.first().bestemmelse.toString()
        if (!medlemskapsperioder.all { it.bestemmelse.toString() == bestemmelse })
            throw FunksjonellException(MEDLEMSKAPSPERIODER_HAR_FORSKJELLIGE_BESTEMMELSER)
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
