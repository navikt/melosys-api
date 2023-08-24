package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import org.apache.commons.beanutils.BeanUtils
import org.springframework.data.util.Pair
import java.time.LocalDate

class UtledMedlemskapsperioder {

    fun lagMedlemskapsperioderForNyVurdering(request: UtledMedlemskapsperiodeNyVurderingRequest): MutableCollection<Medlemskapsperiode> {
        val medlemskapsperioder = mutableListOf<Medlemskapsperiode>()
        if (request.opprinneligeMedlemskapsperioder.isEmpty()) return medlemskapsperioder
        validerOpprinneligeMedlemskapsperioder(request.opprinneligeMedlemskapsperioder)

        request.opprinneligeMedlemskapsperioder.forEach {
            medlemskapsperioder.add(
                (BeanUtils.cloneBean(it) as Medlemskapsperiode).apply { id = null }
            )
        }
        medlemskapsperioder.sortBy { it.fom }

        if (request.opprinneligSøknad.trygdedekning != request.trygdedekning) {
            endreTrygdedekning(medlemskapsperioder, request.opprinneligSøknad.trygdedekning, request)
        }
        val opprinneligPeriode = request.opprinneligSøknad.periode
        if (opprinneligPeriode.fom != request.søknadsperiode.fom) {
            utvidEllerForkortFom(medlemskapsperioder, opprinneligPeriode.fom, request)
        }
        if (opprinneligPeriode.tom != request.søknadsperiode.tom) {
            utvidEllerForkortTom(medlemskapsperioder, opprinneligPeriode.tom, request)
        }
        if (request.opprinneligSøknad.soeknadsland.landkoder.first() != request.arbeidsland) {
            medlemskapsperioder.onEach { it.arbeidsland = request.arbeidsland }
        }

        return medlemskapsperioder
    }

    private fun validerOpprinneligeMedlemskapsperioder(medlemskapsperioder: MutableCollection<Medlemskapsperiode>) {
        if (medlemskapsperioder.any { it.tom == null }) {
            throw FunksjonellException("Den opprinnelige behandlingen sine medlemskapsperioder kan ikke ha åpen sluttdato")
        }
    }

    private fun endreTrygdedekning(
        medlemskapsperioder: MutableList<Medlemskapsperiode>,
        opprinneligTrygdedekning: Trygdedekninger,
        request: UtledMedlemskapsperiodeNyVurderingRequest
    ) {
        val trygdedekningOppdatertMedPensjonsdel =
            (opprinneligTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE && request.trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON)
                || (opprinneligTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER && request.trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER)

        if (trygdedekningOppdatertMedPensjonsdel) {
            medlemskapsperioder
                .filter { !datoErTidligereEnn2ÅrFørMottaksdato(it.tom, request.mottaksdatoSøknad) }
                .onEach { it.trygdedekning = leggTilPensjonsdel(it.trygdedekning) }
        } else {
            val deltePerioder = mutableListOf<Medlemskapsperiode>()
            medlemskapsperioder.forEach {
                if (it.fom.isBefore(request.mottaksdatoSøknad) && it.tom.isAfter(request.mottaksdatoSøknad)) {
                    val splittetPeriode = splitPeriode(Periode(it.fom, it.tom), request.mottaksdatoSøknad)
                    deltePerioder.add(
                        lagPeriode(
                            splittetPeriode.second,
                            it.trygdedekning,
                            it.arbeidsland,
                            it.innvilgelsesresultat
                        )
                    )
                    it.tom = splittetPeriode.first.tom
                }
            }
            medlemskapsperioder.addAll(deltePerioder)
            medlemskapsperioder.sortBy { it.fom }
            medlemskapsperioder
                .filter { it.fom == request.mottaksdatoSøknad || it.fom.isAfter(request.mottaksdatoSøknad) }
                .onEach { it.trygdedekning = request.trygdedekning }
        }
    }

    private fun utvidEllerForkortFom(
        medlemskapsperioder: MutableList<Medlemskapsperiode>,
        opprinneligFom: LocalDate,
        request: UtledMedlemskapsperiodeNyVurderingRequest
    ) {
        val nyFom = request.søknadsperiode.fom

        if (nyFom.isBefore(opprinneligFom)) {
            val utvidetPeriode = Periode(nyFom, opprinneligFom.minusDays(1))
            val utvidetPeriodeRequest = UtledMedlemskapsperioderRequest.av(request, utvidetPeriode)
            medlemskapsperioder.addAll(lagMedlemskapsperioder(utvidetPeriodeRequest))
            medlemskapsperioder.sortBy { it.fom }
        } else if (nyFom.isAfter(opprinneligFom)) {
            val førstePeriodeEtterNyFom = medlemskapsperioder.first { it.tom == null || it.tom.isAfter(nyFom) }
            medlemskapsperioder.subList(0, medlemskapsperioder.indexOf(førstePeriodeEtterNyFom)).clear()
            medlemskapsperioder.filter { it.fom == førstePeriodeEtterNyFom.fom }.onEach { it.fom = nyFom }
        }
    }

    private fun utvidEllerForkortTom(
        medlemskapsperioder: MutableList<Medlemskapsperiode>,
        opprinneligTom: LocalDate?,
        request: UtledMedlemskapsperiodeNyVurderingRequest
    ) {
        val nyTom = request.søknadsperiode.tom

        if (opprinneligTom != null && (nyTom == null || nyTom.isAfter(opprinneligTom))) {
            val utvidetPeriode = Periode(opprinneligTom.plusDays(1), nyTom)
            val utvidetPeriodeRequest = UtledMedlemskapsperioderRequest.av(request, utvidetPeriode)
            medlemskapsperioder.addAll(lagMedlemskapsperioder(utvidetPeriodeRequest))
            medlemskapsperioder.sortBy { it.fom }
        } else if (
            (opprinneligTom == null && nyTom != null) || (opprinneligTom != null && nyTom.isBefore(opprinneligTom))
        ) {
            val sistePeriodeFørNyTom = medlemskapsperioder.last { it.fom.isBefore(nyTom) }
            val sistePeriodeIndex = medlemskapsperioder.indexOf(sistePeriodeFørNyTom)
            medlemskapsperioder.subList(sistePeriodeIndex + 1, medlemskapsperioder.size).clear()
            medlemskapsperioder.filter { it.tom == sistePeriodeFørNyTom.tom }.onEach { it.tom = nyTom }
        }
    }

    fun lagMedlemskapsperioder(request: UtledMedlemskapsperioderRequest): Collection<Medlemskapsperiode> {
        val søknadsperiode = request.søknadsperiode

        val enMånedFørMottaksdato = request.mottaksdatoSøknad.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    request.trygdedekning,
                    request.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        }

        if (datoErTidligereEnn2ÅrFørMottaksdato(søknadsperiode.fom, request.mottaksdatoSøknad)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    request.trygdedekning,
                    request.arbeidsland,
                    InnvilgelsesResultat.AVSLAATT
                )
            )
        }

        return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(request)
    }

    private fun lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(request: UtledMedlemskapsperioderRequest): Collection<Medlemskapsperiode> {
        val søknadsperiode = request.søknadsperiode

        if (erKunPensjonsdel(request.trygdedekning)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    request.trygdedekning,
                    request.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(request.mottaksdatoSøknad)) {
            return lagMedlemskapsperioderForPeriodeFørMottaksdato(søknadsperiode, request)
        }

        val splittetPeriode = splitPeriode(søknadsperiode, request.mottaksdatoSøknad)
        return lagMedlemskapsperioderForPeriodeFørMottaksdato(splittetPeriode.first, request).plus(
            lagPeriode(
                splittetPeriode.second,
                request.trygdedekning,
                request.arbeidsland,
                InnvilgelsesResultat.INNVILGET
            )
        )
    }


    private fun lagMedlemskapsperioderForPeriodeFørMottaksdato(
        periode: ErPeriode,
        request: UtledMedlemskapsperioderRequest
    ): Collection<Medlemskapsperiode> {
        return if (harPensjonsdel(request.trygdedekning)) {
            mutableSetOf(
                lagPeriode(
                    periode,
                    fjernPensjonsdel(request.trygdedekning),
                    request.arbeidsland,
                    InnvilgelsesResultat.AVSLAATT
                ),
                lagPeriode(
                    periode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    request.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        } else mutableSetOf(
            lagPeriode(
                periode,
                request.trygdedekning,
                request.arbeidsland,
                InnvilgelsesResultat.AVSLAATT
            )
        )
    }

    private fun lagPeriode(
        søknadsperiode: ErPeriode,
        trygdedekning: Trygdedekninger,
        arbeidsland: String,
        innvilgelsesResultat: InnvilgelsesResultat
    ): Medlemskapsperiode =
        Medlemskapsperiode().apply {
            this.fom = søknadsperiode.fom
            this.tom = søknadsperiode.tom
            this.arbeidsland = arbeidsland
            this.innvilgelsesresultat = innvilgelsesResultat
            this.medlemskapstype = Medlemskapstyper.PLIKTIG
            this.trygdedekning = trygdedekning
        }


    private fun splitPeriode(periode: ErPeriode, splitFra: LocalDate): Pair<ErPeriode, ErPeriode> =
        Pair.of(
            Periode(periode.fom, splitFra.minusDays(1)),
            Periode(splitFra, periode.tom)
        )


    private fun fjernPensjonsdel(trygdedekning: Trygdedekninger): Trygdedekninger =
        when (trygdedekning) {
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
            else -> throw FunksjonellException("Trygdedekning $trygdedekning har ikke pensjonsdel")
        }

    private fun leggTilPensjonsdel(trygdedekning: Trygdedekninger): Trygdedekninger =
        when (trygdedekning) {
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            else -> trygdedekning
        }


    private fun harPensjonsdel(trygdedekninger: Trygdedekninger): Boolean =
        trygdedekninger == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON || trygdedekninger == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER


    private fun erKunPensjonsdel(trygdedekning: Trygdedekninger): Boolean =
        trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON

    private fun datoErTidligereEnn2ÅrFørMottaksdato(dato: LocalDate, mottaksdato: LocalDate) =
        dato.isBefore(mottaksdato.minusYears(2))

}
