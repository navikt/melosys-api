package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import org.springframework.data.util.Pair
import java.time.LocalDate

object UtledMedlemskapsperioder {

    fun lagMedlemskapsperioderForAndregangsbehandling(
        dto: UtledMedlemskapsperioderDto,
        opprinneligeMedlemskapsperioder: Collection<Medlemskapsperiode>,
        type: Behandlingstyper
    ): Collection<Medlemskapsperiode> {
        if (dto.bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser) {
            return lagMedlemskapsperioderForPliktige(dto)
        }
        return opprinneligeMedlemskapsperioder
            .filter { if (type === Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) it.erInnvilget() || it.erOpphørt() else it.erInnvilget() }
            .map {
                Medlemskapsperiode().apply {
                    fom = it.fom
                    tom = it.tom
                    innvilgelsesresultat = it.innvilgelsesresultat
                    medlemskapstype = UtledMedlemskapstype.av(dto.bestemmelse)
                    trygdedekning =
                        if (LovligeKombinasjonerTrygdedekningBestemmelse.erGyldigKombinasjon(dto.bestemmelse, it.trygdedekning)) it.trygdedekning
                        else dto.trygdedekning
                    medlPeriodeID = it.medlPeriodeID
                    bestemmelse = if (it.erOpphørt()) it.bestemmelse else dto.bestemmelse
                }
            }
    }

    fun lagMedlemskapsperioder(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        return when {
            bestemmelseErParagraf(dto.bestemmelse, "2_7") ->
                lagMedlemskapsperioderFor2_7(dto)

            bestemmelseErParagraf(dto.bestemmelse, "2_8") ->
                lagMedlemskapsperioderFor2_8(dto)

            dto.bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser ->
                lagMedlemskapsperioderForPliktige(dto)

            else -> throw FunksjonellException("Støtter ikke bestemmelse ${dto.bestemmelse}")
        }
    }

    fun lagMedlemskapsperioderForPensjonist(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        return when {
            bestemmelseErParagraf(dto.bestemmelse, "2_7") ->
                lagMedlemskapsperioderPensjonistFor2_7(dto)

            bestemmelseErParagraf(dto.bestemmelse, "2_8") ->
                lagMedlemskapsperioderPensjonistFor2_8(dto)

            else -> throw FunksjonellException("Støtter ikke bestemmelse ${dto.bestemmelse}")
        }
    }

    private fun lagMedlemskapsperioderPensjonistFor2_7(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode
        val mottaksdato = dto.mottaksdatoSøknadNotNull
        val enMånedFørMottaksdato = mottaksdato.minusMonths(1)
        val trygdedekning = dto.trygdedekning
        return when {
            //Scenario 1
            søknadsperiode.fom == mottaksdato ||
                søknadsperiode.fom.isAfter(mottaksdato) ||
                søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && listOf(
                Trygdedekninger.FULL_DEKNING,
                Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
            ).contains(trygdedekning) -> setOf(
                lagPeriode(søknadsperiode, trygdedekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse)
            )
            //Scenario 2 og 3
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && søknadsperiode.tom.isAfter(mottaksdato) -> {
                val erScenario2 = Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER == trygdedekning
                val erScenario3 = Trygdedekninger.FULL_DEKNING == trygdedekning

                if (erScenario2) {
                    val førstePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                    val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)
                    return setOf(
                        lagPeriode(
                            førstePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.AVSLAATT,
                            dto.bestemmelse
                        ),
                        lagPeriode(
                            andrePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.INNVILGET,
                            dto.bestemmelse
                        )
                    )
                }
                if (erScenario3) {
                    val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)
                    return setOf(
                        lagPeriode(søknadsperiode, Trygdedekninger.FULL_DEKNING, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse),
                        lagPeriode(
                            andrePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.INNVILGET,
                            dto.bestemmelse
                        )
                    )
                }
                return setOf()
            }
            //Scenario 4
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) &&
                søknadsperiode.tom.isBefore(mottaksdato) &&
                listOf(
                    Trygdedekninger.FULL_DEKNING,
                    Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                ).contains(trygdedekning) -> setOf(
                lagPeriode(søknadsperiode, trygdedekning, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse)
            )
            else -> emptySet()
        }
    }

    private fun lagMedlemskapsperioderFor2_7(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        val enMånedFørMottaksdato = dto.mottaksdatoSøknadNotNull.minusMonths(1)
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

        val splittetPeriode = splitPeriode(søknadsperiode, dto.mottaksdatoSøknadNotNull)
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
                if (dto.bestemmelse == Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO) Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL else Trygdedekninger.FULL_DEKNING_FTRL,
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            )
        )
    }

    private fun lagMedlemskapsperioderFor2_8(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode
        val enMånedFørMottaksdato = dto.mottaksdatoSøknadNotNull.minusMonths(1)

        if (datoErTidligereEnn2ÅrFørMottaksdato(søknadsperiode.fom, dto.mottaksdatoSøknadNotNull)) {
            return lagAvslåttPeriode(dto)
        }

        if (søknadsperiode.fom.isEqualOrAfter(enMånedFørMottaksdato)) {
            return lagMedlemskapsperioderPeriodeStarterMindreEnnEnMånedFørMottaksdato(dto)
        }

        return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(dto)
    }

    private fun lagMedlemskapsperioderPensjonistFor2_8(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode
        val mottaksdato = dto.mottaksdatoSøknadNotNull
        val enMånedFørMottaksdato = mottaksdato.minusMonths(1)
        val toÅrTilbakeMottaksdato = mottaksdato.minusYears(2)
        val toMånedTilbakeMottaksdato = mottaksdato.minusMonths(2)
        val toÅrTilbakeITid = LocalDate.now().minusYears(2)
        val trygdedekning = dto.trygdedekning
        return when {
            // Scenario 1 og 2
            søknadsperiode.fom == mottaksdato ||
                søknadsperiode.fom.isAfter(mottaksdato) ||
                søknadsperiode.fom.isBefore(enMånedFørMottaksdato) -> {
                val scenario1 = listOf(
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                ).contains(dto.trygdedekning)

                val scenario2 = listOf(
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE,
                ).contains(dto.trygdedekning)
                val splittetPeriode = splitPeriode(søknadsperiode, mottaksdato)

                val dekning = when (trygdedekning) {
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                    else -> dto.trygdedekning
                }

                return if (scenario1) setOf(
                    lagPeriode(søknadsperiode, dto.trygdedekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse)
                ) else if (scenario2) setOf(
                    lagPeriode(
                        splittetPeriode.first,
                        Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                        InnvilgelsesResultat.AVSLAATT,
                        dto.bestemmelse
                    ),
                    lagPeriode(splittetPeriode.second, dekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse)
                ) else {
                    setOf()
                }
            }
            // Scenario 3
            søknadsperiode.fom.isBefore(toÅrTilbakeMottaksdato) -> setOf(
                lagPeriode(søknadsperiode, dto.trygdedekning, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse)
            )
            // Scenario 4, 5
            søknadsperiode.fom.isBefore(toMånedTilbakeMottaksdato) &&
                !søknadsperiode.fom.isBefore(toÅrTilbakeMottaksdato) -> {
                return when (trygdedekning) {
                    //Scenario 4
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> setOf(
                        lagPeriode(søknadsperiode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.INNVILGET, dto.bestemmelse)
                    )
                    //Scenario 5
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE -> setOf(
                        lagPeriode(søknadsperiode, Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse),
                        lagPeriode(søknadsperiode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.INNVILGET, dto.bestemmelse)
                    )

                    else -> setOf()
                }
            }
            // Scenario 6, 7
            søknadsperiode.fom.isBefore(toMånedTilbakeMottaksdato) &&
                !søknadsperiode.fom.isBefore(toÅrTilbakeITid) -> {
                return when (trygdedekning) {
                    //Scenario 6
                    in listOf(
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                    ) -> {
                        val førstePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                        val andrePeriode = Periode(mottaksdato.plusDays(1), søknadsperiode.tom)
                        val tredjePeriode = Periode(mottaksdato, søknadsperiode.tom)
                        val førstePeriodeDekning = when (trygdedekning) {
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                            else -> trygdedekning
                        }
                        return setOf(
                            lagPeriode(førstePeriode, førstePeriodeDekning, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse),
                            lagPeriode(andrePeriode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.INNVILGET, dto.bestemmelse),
                            lagPeriode(tredjePeriode, dto.trygdedekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse),
                        )
                    }
                    //Scenario 7
                    in listOf(
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE
                    ) -> {
                        val andrePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                        val tredjePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                        val fjerdePeriode = Periode(mottaksdato, søknadsperiode.tom)
                        val andrePeriodeDekning = when (trygdedekning) {
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                            else -> trygdedekning
                        }
                        val fjerdePeriodeDekning = when (trygdedekning) {
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                            else -> trygdedekning
                        }
                        return setOf(
                            lagPeriode(
                                søknadsperiode,
                                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                                InnvilgelsesResultat.AVSLAATT,
                                dto.bestemmelse
                            ),
                            lagPeriode(andrePeriode, andrePeriodeDekning, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse),
                            lagPeriode(
                                tredjePeriode,
                                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                                InnvilgelsesResultat.INNVILGET,
                                dto.bestemmelse
                            ),
                            lagPeriode(fjerdePeriode, fjerdePeriodeDekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse),
                        )
                    }

                    else -> setOf()
                }
            }
            //Scenario 8
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
            ).contains(trygdedekning) -> {
                val førstePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)
                return setOf(
                    lagPeriode(førstePeriode, trygdedekning, InnvilgelsesResultat.AVSLAATT, dto.bestemmelse),
                    lagPeriode(andrePeriode, trygdedekning, InnvilgelsesResultat.INNVILGET, dto.bestemmelse),

                    )
            }

            else -> setOf()

        }
    }


    private fun lagInnvilgetPeriode(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> =
        setOf(
            lagPeriode(
                dto.søknadsperiode,
                dto.trygdedekning,
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            )
        )

    private fun lagAvslåttPeriode(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> =
        setOf(
            lagPeriode(
                dto.søknadsperiode,
                dto.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            )
        )


    private fun lagMedlemskapsperioderPeriodeStarterMindreEnnEnMånedFørMottaksdato(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        if (dto.trygdedekning.er2_9FørsteLeddMedYrkesskade()) {
            return setOf(
                lagPeriode(
                    dto.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                    InnvilgelsesResultat.AVSLAATT,
                    dto.bestemmelse
                )
            ).plus(
                lagPeriode(
                    dto.søknadsperiode,
                    dto.trygdedekning.utenYrkesskadedel(),
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
                )
            )
        }

        return lagInnvilgetPeriode(dto)
    }

    private fun lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(dto: UtledMedlemskapsperioderDto): Collection<Medlemskapsperiode> {
        val søknadsperiode = dto.søknadsperiode

        if (dto.trygdedekning.erKunPensjonsdel()) {
            return lagInnvilgetPeriode(dto)
        }

        if (dto.trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE) {
            return setOf(
                lagPeriode(
                    dto.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                    InnvilgelsesResultat.AVSLAATT,
                    dto.bestemmelse
                ),
                lagPeriode(
                    dto.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    InnvilgelsesResultat.INNVILGET,
                    dto.bestemmelse
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(dto.mottaksdatoSøknad)) {
            return lagMedlemskapsperioderForPeriodeFørMottaksdato(søknadsperiode, dto)
        }

        val splittetPeriode = splitPeriode(søknadsperiode, dto.mottaksdatoSøknadNotNull)
        if (dto.trygdedekning in listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE
            )
        ) {
            return lagSplittetYrkesskadeperioder(dto, splittetPeriode)
        }
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
    ): Collection<Medlemskapsperiode> =
        if (dto.trygdedekning.harPensjonsdel()) {
            setOf(
                lagPeriode(
                    periode,
                    dto.trygdedekning.utenPensjonsdel(),
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
        } else setOf(
            lagPeriode(
                periode,
                dto.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            )
        )

    private fun lagSplittetYrkesskadeperioder(
        dto: UtledMedlemskapsperioderDto,
        splittetPeriode: Pair<ErPeriode, ErPeriode>
    ): Collection<Medlemskapsperiode> =
        setOf(
            lagPeriode(
                dto.søknadsperiode,
                Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            ),
            lagPeriode(
                splittetPeriode.first,
                dto.trygdedekning.utenYrkesskadedel().utenPensjonsdel(),
                InnvilgelsesResultat.AVSLAATT,
                dto.bestemmelse
            ),
            lagPeriode(
                splittetPeriode.first,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            ),
            lagPeriode(
                splittetPeriode.second,
                dto.trygdedekning.utenYrkesskadedel(),
                InnvilgelsesResultat.INNVILGET,
                dto.bestemmelse
            )
        )

    private fun lagPeriode(
        søknadsperiode: ErPeriode,
        trygdedekning: Trygdedekninger,
        innvilgelsesResultat: InnvilgelsesResultat,
        bestemmelse: Bestemmelse
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

    private fun datoErTidligereEnn2ÅrFørMottaksdato(dato: LocalDate, mottaksdato: LocalDate) =
        dato.isBefore(mottaksdato.minusYears(2))

    private fun bestemmelseErParagraf(bestemmelse: Bestemmelse, paragraf: String): Boolean =
        bestemmelse.kode.startsWith("FTRL_KAP2_$paragraf")


    private fun Trygdedekninger.harPensjonsdel(): Boolean =
        this in listOf(
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        )

    private fun Trygdedekninger.erKunPensjonsdel(): Boolean =
        this == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON

    private fun Trygdedekninger.er2_9FørsteLeddMedYrkesskade(): Boolean =
        listOf(
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE,
        ).contains(this)

    private fun Trygdedekninger.utenPensjonsdel(): Trygdedekninger =
        when (this) {
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
            else -> throw FunksjonellException("Trygdedekning $this har ikke pensjonsdel")
        }

    private fun Trygdedekninger.utenYrkesskadedel(): Trygdedekninger =
        when (this) {
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE -> Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            else -> throw FunksjonellException("Trygdedekning $this har ikke yrkesskadedel")
        }

    private fun LocalDate.isEqualOrAfter(localDate: LocalDate): Boolean = this.isEqual(localDate) || this.isAfter(localDate)
}

