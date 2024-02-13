package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerMedlemskapsperiodeRegler
import org.springframework.data.util.Pair
import java.time.LocalDate

object UtledMedlemskapsperioder {

    fun lagMedlemskapsperioderForAndregangsbehandling(
        opprinneligBehandlingsresultat: Behandlingsresultat,
        nyBestemmelse: Folketrygdloven_kap2_bestemmelser,
        nyTrygdedekning: Trygdedekninger,
        type: Behandlingstyper
    ): Collection<Medlemskapsperiode> =
        opprinneligBehandlingsresultat.medlemAvFolketrygden.medlemskapsperioder
            .filter { if (type === Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) it.erInnvilget() || it.erOpphørt() else it.erInnvilget() }
            .map {
                Medlemskapsperiode().apply {
                    fom = it.fom
                    tom = it.tom
                    innvilgelsesresultat = it.innvilgelsesresultat
                    medlemskapstype = UtledMedlemskapstype.av(nyBestemmelse)
                    trygdedekning =
                        if (LovligeKombinasjonerMedlemskapsperiodeRegler.erGyldigKombinasjon(nyBestemmelse, it.trygdedekning)) it.trygdedekning
                        else nyTrygdedekning
                    medlPeriodeID = it.medlPeriodeID
                    bestemmelse = if (it.erOpphørt()) it.bestemmelse else nyBestemmelse
                }
            }

    fun lagMedlemskapsperioder(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        if (bestemmelseErParagraf(dto.bestemmelse, "2_7")) {
            return lagMedlemskapsperioderFor2_7(dto)
        } else if (bestemmelseErParagraf(dto.bestemmelse, "2_8")) {
            return lagMedlemskapsperioderFor2_8(dto)
        } else if (dto.bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser) {
            return lagMedlemskapsperioderForPliktige(dto)
        }
        throw FunksjonellException("Støtter ikke bestemmelse ${dto.bestemmelse}")
    }

    private fun lagMedlemskapsperioderFor2_7(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        val enMånedFørMottaksdato = dto.mottaksdatoSøknad.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(dto.mottaksdatoSøknad)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    InnvilgelsesResultat.AVSLAATT,
                    dto.bestemmelse
                )
            )
        }

        val splittetPeriode = splitPeriode(søknadsperiode, dto.mottaksdatoSøknad)
        return setOf(
            lagPeriode(
                splittetPeriode.first,
                dto.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            ),
            lagPeriode(
                splittetPeriode.second,
                dto.trygdedekning,
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            )
        )
    }

    private fun lagMedlemskapsperioderForPliktige(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        return setOf(
            lagPeriode(
                dto.søknadsperiode,
                Trygdedekninger.FULL_DEKNING_FTRL,
                dto.arbeidsland,
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            )
        )
    }

    private fun lagMedlemskapsperioderFor2_8(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        val enMånedFørMottaksdato = dto.mottaksdatoSøknad.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
                )
            )
        }

        if (datoErTidligereEnn2ÅrFørMottaksdato(søknadsperiode.fom, dto.mottaksdatoSøknad)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    dto.trygdedekning,
                    InnvilgelsesResultat.AVSLAATT,
                    dto.bestemmelse
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
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
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
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
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
                    InnvilgelsesResultat.AVSLAATT,
                    dto.bestemmelse
                ),
                lagPeriode(
                    periode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
                )
            )
        } else mutableSetOf(
            lagPeriode(
                periode,
                dto.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            )
        )
    }

    private fun lagPeriode(
        søknadsperiode: ErPeriode,
        trygdedekning: Trygdedekninger,
        innvilgelsesResultat: InnvilgelsesResultat,
        bestemmelse: Folketrygdloven_kap2_bestemmelser
    ): Medlemskapsperiode =
        Medlemskapsperiode().apply {
            this.fom = søknadsperiode.fom
            this.tom = søknadsperiode.tom
            this.innvilgelsesresultat = innvilgelsesResultat
            this.medlemskapstype = UtledMedlemskapstype.av(bestemmelse)
            this.trygdedekning = trygdedekning
            this.bestemmelse = bestemmelse
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

    private fun bestemmelseErParagraf(bestemmelse: Folketrygdloven_kap2_bestemmelser, paragraf: String): Boolean =
        bestemmelse.kode.startsWith("FTRL_KAP2_$paragraf")
}
