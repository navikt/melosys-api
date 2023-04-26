package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import org.springframework.data.util.Pair
import java.time.LocalDate

class UtledMedlemskapsperioder {

    fun lagMedlemskapsperioder(request: UtledMedlemskapsperioderRequest): Collection<Medlemskapsperiode> {
        val søknadsperiode = request.søknadsperiode

        val enMånedFørMottaksdato = request.mottaksdatoSøknad.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    request.trygdedekning,
                    request.bestemmelse,
                    request.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        }

        if (søknadsperiode.fom.isBefore(request.mottaksdatoSøknad.minusYears(2))) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    request.trygdedekning,
                    request.bestemmelse,
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
                    request.bestemmelse,
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
                request.bestemmelse,
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
                    request.bestemmelse,
                    request.arbeidsland,
                    InnvilgelsesResultat.AVSLAATT
                ),
                lagPeriode(
                    periode,
                    Trygdedekninger.PENSJONSDEL,
                    request.bestemmelse,
                    request.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        } else mutableSetOf(
            lagPeriode(
                periode,
                request.trygdedekning,
                request.bestemmelse,
                request.arbeidsland,
                InnvilgelsesResultat.AVSLAATT
            )
        )
    }

    private fun lagPeriode(
        søknadsperiode: ErPeriode,
        trygdedekning: Trygdedekninger,
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        arbeidsland: String,
        innvilgelsesResultat: InnvilgelsesResultat
    ): Medlemskapsperiode =
        Medlemskapsperiode().apply {
            this.fom = søknadsperiode.fom
            this.tom = søknadsperiode.tom
            this.arbeidsland = arbeidsland
            this.bestemmelse = bestemmelse
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
            Trygdedekninger.HELSE_OG_PENSJONSDEL -> Trygdedekninger.HELSEDEL
            Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER -> Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER
            else -> throw FunksjonellException("Trygdedekning $trygdedekning har ikke pensjonsdel")
        }


    private fun harPensjonsdel(trygdedekninger: Trygdedekninger): Boolean =
        trygdedekninger == Trygdedekninger.HELSE_OG_PENSJONSDEL || trygdedekninger == Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER


    private fun erKunPensjonsdel(trygdedekning: Trygdedekninger): Boolean =
        trygdedekning == Trygdedekninger.PENSJONSDEL

}
