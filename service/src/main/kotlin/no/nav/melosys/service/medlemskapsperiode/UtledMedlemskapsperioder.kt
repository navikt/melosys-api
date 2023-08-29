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

    fun lagMedlemskapsperioderForNyVurdering(dto: UtledMedlemskapsperiodeNyVurderingDto): Collection<Medlemskapsperiode> {
        if (dto.opprinneligeMedlemskapsperioder.isEmpty()) {
            return lagMedlemskapsperioder(UtledMedlemskapsperioderDto.av(dto))
        }

        validerOpprinneligeMedlemskapsperioder(dto.opprinneligeMedlemskapsperioder)

        val medlemskapsperioder = mutableListOf<Medlemskapsperiode>()
        dto.opprinneligeMedlemskapsperioder.forEach {
            medlemskapsperioder.add(
                (BeanUtils.cloneBean(it) as Medlemskapsperiode).apply { id = null }
            )
        }
        medlemskapsperioder.sortBy { it.fom }

        if (dto.opprinneligSøknad.trygdedekning != dto.trygdedekning) {
            endreTrygdedekning(medlemskapsperioder, dto.opprinneligSøknad.trygdedekning, dto)
        }
        val opprinneligPeriode = dto.opprinneligSøknad.periode
        if (opprinneligPeriode.fom != dto.søknadsperiode.fom) {
            utvidEllerForkortFom(medlemskapsperioder, opprinneligPeriode.fom, dto)
        }
        if (opprinneligPeriode.tom != dto.søknadsperiode.tom) {
            utvidEllerForkortTom(medlemskapsperioder, opprinneligPeriode.tom, dto)
        }
        if (dto.opprinneligSøknad.soeknadsland.landkoder.first() != dto.arbeidsland) {
            medlemskapsperioder.onEach { it.arbeidsland = dto.arbeidsland }
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
        dto: UtledMedlemskapsperiodeNyVurderingDto
    ) {
        if (trygdedekningOppdatertMedPensjonsdel(opprinneligTrygdedekning, dto.trygdedekning)) {
            medlemskapsperioder
                .filter { !datoErTidligereEnn2ÅrFørMottaksdato(it.tom, dto.mottaksdatoSøknad) }
                .onEach { it.trygdedekning = leggTilPensjonsdel(it.trygdedekning) }
        } else {
            val deltePerioder = mutableListOf<Medlemskapsperiode>()
            medlemskapsperioder.forEach {
                if (it.fom.isBefore(dto.mottaksdatoSøknad) && it.tom.isAfter(dto.mottaksdatoSøknad)) {
                    val splittetPeriode = splitPeriode(Periode(it.fom, it.tom), dto.mottaksdatoSøknad)
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
                .filter { it.fom == dto.mottaksdatoSøknad || it.fom.isAfter(dto.mottaksdatoSøknad) }
                .onEach { it.trygdedekning = dto.trygdedekning }
        }
    }

    private fun utvidEllerForkortFom(
        medlemskapsperioder: MutableList<Medlemskapsperiode>,
        opprinneligFom: LocalDate,
        dto: UtledMedlemskapsperiodeNyVurderingDto
    ) {
        val nyFom = dto.søknadsperiode.fom

        if (nyFom.isBefore(opprinneligFom)) {
            val utvidetPeriode = Periode(nyFom, opprinneligFom.minusDays(1))
            val utvidetPeriodeDto = UtledMedlemskapsperioderDto.av(dto, utvidetPeriode)
            medlemskapsperioder.addAll(lagMedlemskapsperioder(utvidetPeriodeDto))
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
        dto: UtledMedlemskapsperiodeNyVurderingDto
    ) {
        val nyTom = dto.søknadsperiode.tom

        if (opprinneligTom != null && (nyTom == null || nyTom.isAfter(opprinneligTom))) {
            val utvidetPeriode = Periode(opprinneligTom.plusDays(1), nyTom)
            val utvidetPeriodeDto = UtledMedlemskapsperioderDto.av(dto, utvidetPeriode)
            medlemskapsperioder.addAll(lagMedlemskapsperioder(utvidetPeriodeDto))
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
            this.medlemskapstype = Medlemskapstyper.PLIKTIG
            this.trygdedekning = trygdedekning
        }


    private fun splitPeriode(periode: ErPeriode, splitFra: LocalDate): Pair<ErPeriode, ErPeriode> =
        Pair.of(
            Periode(periode.fom, splitFra.minusDays(1)),
            Periode(splitFra, periode.tom)
        )

    private fun trygdedekningOppdatertMedPensjonsdel(
        opprinneligTrygdedekning: Trygdedekninger,
        nyTrygdedekning: Trygdedekninger
    ): Boolean {
        if (opprinneligTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            && nyTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        ) {
            return true
        }
        if (opprinneligTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
            && nyTrygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        ) {
            return true
        }
        return false
    }

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
