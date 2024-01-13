package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import org.springframework.data.util.Pair
import java.time.LocalDate

class UtledMedlemskapsperioder {

    fun lagMedlemskapsperioderForNyVurdering(opprinneligBehandlingsresultat: Behandlingsresultat): Collection<Medlemskapsperiode> =
        opprinneligBehandlingsresultat.medlemAvFolketrygden.medlemskapsperioder
            .filter { it.erInnvilget() }
            .map {
                Medlemskapsperiode().apply {
                    fom = it.fom
                    tom = it.tom
                    arbeidsland = it.arbeidsland
                    innvilgelsesresultat = it.innvilgelsesresultat
                    medlemskapstype = it.medlemskapstype
                    trygdedekning = it.trygdedekning
                    medlPeriodeID = it.medlPeriodeID
                }
            }

    fun lagMedlemskapsperioderForManglendeInnbetaling(opprinneligBehandlingsresultat: Behandlingsresultat): Collection<Medlemskapsperiode> =
        opprinneligBehandlingsresultat.medlemAvFolketrygden.medlemskapsperioder
            .filter { it.erInnvilget() || it.erOpphørt() }
            .map {
                Medlemskapsperiode().apply {
                    fom = it.fom
                    tom = it.tom
                    arbeidsland = it.arbeidsland
                    innvilgelsesresultat = it.innvilgelsesresultat
                    medlemskapstype = it.medlemskapstype
                    trygdedekning = it.trygdedekning
                    medlPeriodeID = it.medlPeriodeID
                }
            }

    fun lagMedlemskapsperioder(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        val enMånedFørMottaksdato = dto.mottaksdatoSøknad.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    dto.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        }

        if (datoErTidligereEnn2ÅrFørMottaksdato(søknadsperiode.fom, dto.mottaksdatoSøknad)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    dto.arbeidsland,
                    InnvilgelsesResultat.AVSLAATT
                )
            )
        }

        return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(dto)
    }

    private fun lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        if (erKunPensjonsdel(dto.trygdedekning)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    dto.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(dto.mottaksdatoSøknad)) {
            return lagMedlemskapsperioderForPeriodeFørMottaksdato(søknadsperiode, dto)
        }

        val splittetPeriode = splitPeriode(søknadsperiode, dto.mottaksdatoSøknad)
        return lagMedlemskapsperioderForPeriodeFørMottaksdato(splittetPeriode.first, dto).plus(
            lagPeriode(
                splittetPeriode.second,
                dto.trygdedekning,
                dto.arbeidsland,
                InnvilgelsesResultat.INNVILGET
            )
        )
    }


    private fun lagMedlemskapsperioderForPeriodeFørMottaksdato(
        periode: ErPeriode,
        dto: UtledMedlemskapsperioderDto
    ): Collection<Medlemskapsperiode> {
        return if (harPensjonsdel(dto.trygdedekning)) {
            mutableSetOf(
                lagPeriode(
                    periode,
                    fjernPensjonsdel(dto.trygdedekning),
                    dto.arbeidsland,
                    InnvilgelsesResultat.AVSLAATT
                ),
                lagPeriode(
                    periode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    dto.arbeidsland,
                    InnvilgelsesResultat.INNVILGET
                )
            )
        } else mutableSetOf(
            lagPeriode(
                periode,
                dto.trygdedekning,
                dto.arbeidsland,
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
            this.medlemskapstype = Medlemskapstyper.FRIVILLIG
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

    private fun harPensjonsdel(trygdedekninger: Trygdedekninger): Boolean =
        trygdedekninger == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON || trygdedekninger == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER


    private fun erKunPensjonsdel(trygdedekning: Trygdedekninger): Boolean =
        trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON

    private fun datoErTidligereEnn2ÅrFørMottaksdato(dato: LocalDate, mottaksdato: LocalDate) =
        dato.isBefore(mottaksdato.minusYears(2))

}
