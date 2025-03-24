package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import org.springframework.data.util.Pair
import java.time.LocalDate

object UtledMedlemskapsperioder {

    fun lagMedlemskapsperioderForAndregangsbehandling(
        grunnlag: UtledMedlemskapsperioderGrunnlag,
        opprinneligeMedlemskapsperioder: Collection<Medlemskapsperiode>,
        type: Behandlingstyper
    ): Collection<Medlemskapsperiode> {
        if (grunnlag.bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser) {
            return lagMedlemskapsperioderForPliktige(grunnlag)
        }
        return opprinneligeMedlemskapsperioder
            .filter { if (type === Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) it.erInnvilget() || it.erOpphørt() else it.erInnvilget() }
            .map {
                Medlemskapsperiode().apply {
                    fom = it.fom
                    tom = it.tom
                    innvilgelsesresultat = it.innvilgelsesresultat
                    medlemskapstype = UtledMedlemskapstype.av(grunnlag.bestemmelse)
                    trygdedekning =
                        if (LovligeKombinasjonerTrygdedekningBestemmelse.erGyldigKombinasjon(grunnlag.bestemmelse, it.trygdedekning)) it.trygdedekning
                        else grunnlag.trygdedekning
                    medlPeriodeID = it.medlPeriodeID
                    bestemmelse = if (it.erOpphørt()) it.bestemmelse else grunnlag.bestemmelse
                }
            }
    }

    fun lagMedlemskapsperioder(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> = when {
        bestemmelseErParagraf(grunnlag.bestemmelse, "2_7") -> lagMedlemskapsperioderFor2_7(grunnlag)

        bestemmelseErParagraf(grunnlag.bestemmelse, "2_8") -> lagMedlemskapsperioderFor2_8(grunnlag)

        grunnlag.bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser -> lagMedlemskapsperioderForPliktige(grunnlag)

        else -> throw FunksjonellException("Støtter ikke bestemmelse ${grunnlag.bestemmelse}")
    }

    private fun lagMedlemskapsperioderFor2_7(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        if (grunnlag.behandlingstema == Behandlingstema.PENSJONIST) {
            return lagMedlemskapsperioderPensjonistFor2_7(grunnlag)
        }

        val søknadsperiode = grunnlag.søknadsperiode

        val enMånedFørMottaksdato = grunnlag.mottaksdatoSøknadNotNull.minusMonths(1)
        if (søknadsperiode.fom == enMånedFørMottaksdato || søknadsperiode.fom.isAfter(enMånedFørMottaksdato)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    grunnlag.trygdedekning,
                    InnvilgelsesResultat.INNVILGET,
                    grunnlag.bestemmelse
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(grunnlag.mottaksdatoSøknad)) {
            return setOf(
                lagPeriode(
                    søknadsperiode,
                    grunnlag.trygdedekning,
                    InnvilgelsesResultat.AVSLAATT,
                    grunnlag.bestemmelse
                )
            )
        }

        val splittetPeriode = splitPeriode(søknadsperiode, grunnlag.mottaksdatoSøknadNotNull)
        return setOf(
            lagPeriode(
                splittetPeriode.first,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                grunnlag.bestemmelse
            ),
            lagPeriode(
                splittetPeriode.second,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.INNVILGET,
                grunnlag.bestemmelse
            )
        )
    }

    private fun lagMedlemskapsperioderPensjonistFor2_7(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        val søknadsperiode = grunnlag.søknadsperiode
        val mottaksdato = grunnlag.mottaksdatoSøknadNotNull
        val enMånedFørMottaksdato = mottaksdato.minusMonths(1)
        val trygdedekning = grunnlag.trygdedekning
        return when {
            //Scenario 1
            mottaksdato == søknadsperiode.fom ||
                mottaksdato.isBefore(søknadsperiode.fom) ||
                mottaksdato.isBefore(søknadsperiode.fom.plusMonths(1)) && listOf(
                Trygdedekninger.FULL_DEKNING_FTRL,
                Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
            ).contains(trygdedekning) -> setOf(
                lagPeriode(
                    søknadsperiode,
                    Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                    InnvilgelsesResultat.INNVILGET,
                    grunnlag.bestemmelse
                )
            )
            //Scenario 2 og 3
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && søknadsperiode.tom.isAfter(mottaksdato) -> {
                val erScenario2 = Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER == trygdedekning
                val erScenario3 = Trygdedekninger.FULL_DEKNING_FTRL == trygdedekning

                if (erScenario2) {
                    val førstePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                    val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)
                    return setOf(
                        lagPeriode(
                            førstePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.AVSLAATT,
                            grunnlag.bestemmelse
                        ),
                        lagPeriode(
                            andrePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.INNVILGET,
                            grunnlag.bestemmelse
                        )
                    )
                }
                if (erScenario3) {
                    val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)
                    return setOf(
                        lagPeriode(søknadsperiode, Trygdedekninger.FULL_DEKNING_FTRL, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse),
                        lagPeriode(
                            andrePeriode,
                            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
                            InnvilgelsesResultat.INNVILGET,
                            grunnlag.bestemmelse
                        )
                    )
                }
                return emptySet()
            }
            //Scenario 4
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) &&
                søknadsperiode.tom.isBefore(mottaksdato) &&
                listOf(
                    Trygdedekninger.FULL_DEKNING_FTRL,
                    Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                ).contains(trygdedekning) -> setOf(
                lagPeriode(søknadsperiode, trygdedekning, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse)
            )

            else -> emptySet()
        }
    }

    private fun lagMedlemskapsperioderForPliktige(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        return setOf(
            lagPeriode(
                grunnlag.søknadsperiode,
                if (grunnlag.bestemmelse == Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO) Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL else Trygdedekninger.FULL_DEKNING_FTRL,
                InnvilgelsesResultat.INNVILGET,
                grunnlag.bestemmelse
            )
        )
    }

    private fun lagMedlemskapsperioderFor2_8(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        if (grunnlag.behandlingstema == Behandlingstema.PENSJONIST) {
            return lagMedlemskapsperioderPensjonistFor2_8(grunnlag)
        }

        val søknadsperiode = grunnlag.søknadsperiode
        val enMånedFørMottaksdato = grunnlag.mottaksdatoSøknadNotNull.minusMonths(1)

        if (datoErTidligereEnn2ÅrFørMottaksdato(søknadsperiode.fom, grunnlag.mottaksdatoSøknadNotNull)) {
            return lagAvslåttPeriode(grunnlag)
        }

        if (søknadsperiode.fom.isEqualOrAfter(enMånedFørMottaksdato)) {
            return lagMedlemskapsperioderPeriodeStarterMindreEnnEnMånedFørMottaksdato(grunnlag)
        }

        return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(grunnlag)
    }

    private fun lagMedlemskapsperioderPensjonistFor2_8(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        val søknadsperiode = grunnlag.søknadsperiode
        val mottaksdato = grunnlag.mottaksdatoSøknadNotNull
        val enMånedFørMottaksdato = mottaksdato.minusMonths(1)
        val trygdedekning = grunnlag.trygdedekning
        return when {
            // Scenario 1 og 2
            mottaksdato == søknadsperiode.fom ||
                mottaksdato.isBefore(søknadsperiode.fom) ||
                mottaksdato.isBefore(søknadsperiode.fom.plusMonths(1)) -> {
                val scenario1 = listOf(
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                ).contains(grunnlag.trygdedekning)

                val scenario2 = listOf(
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                ).contains(grunnlag.trygdedekning)

                return if (scenario1) setOf(
                    lagPeriode(søknadsperiode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.INNVILGET, grunnlag.bestemmelse)
                ) else if (scenario2) setOf(
                    lagPeriode(
                        søknadsperiode,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                        InnvilgelsesResultat.INNVILGET,
                        grunnlag.bestemmelse
                    ),
                    lagPeriode(
                        søknadsperiode,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                        InnvilgelsesResultat.AVSLAATT,
                        grunnlag.bestemmelse
                    )
                ) else {
                    emptySet()
                }
            }
            // Scenario 3 og 4
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && !søknadsperiode.tom.isBefore(mottaksdato) -> {
                val scenario3 =
                    listOf(
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
                    ).contains(trygdedekning)

                val scenario4 =
                    listOf(
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
                    ).contains(trygdedekning)

                val førstePeriode = Periode(søknadsperiode.fom, mottaksdato.minusDays(1))
                val andrePeriode = Periode(mottaksdato, søknadsperiode.tom)

                return if (scenario3) setOf(
                    lagPeriode(førstePeriode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse),
                    lagPeriode(andrePeriode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.INNVILGET, grunnlag.bestemmelse)
                ) else if (scenario4) setOf(
                    lagPeriode(førstePeriode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse),
                    lagPeriode(andrePeriode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.INNVILGET, grunnlag.bestemmelse),
                    lagPeriode(søknadsperiode, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse)
                ) else emptySet()

            }
            // Scenario 5
            søknadsperiode.fom.isBefore(enMånedFørMottaksdato) && søknadsperiode.fom.isBefore(mottaksdato) && listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
            ).contains(trygdedekning) -> setOf(
                lagPeriode(søknadsperiode, trygdedekning, InnvilgelsesResultat.AVSLAATT, grunnlag.bestemmelse),
            )

            else -> emptySet()
        }
    }


    private fun lagInnvilgetPeriode(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> =
        setOf(
            lagPeriode(
                grunnlag.søknadsperiode,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.INNVILGET,
                grunnlag.bestemmelse
            )
        )

    private fun lagAvslåttPeriode(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> =
        setOf(
            lagPeriode(
                grunnlag.søknadsperiode,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                grunnlag.bestemmelse
            )
        )


    private fun lagMedlemskapsperioderPeriodeStarterMindreEnnEnMånedFørMottaksdato(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        if (grunnlag.trygdedekning.er2_9FørsteLeddMedYrkesskade()) {
            return setOf(
                lagPeriode(
                    grunnlag.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                    InnvilgelsesResultat.AVSLAATT,
                    grunnlag.bestemmelse
                )
            ).plus(
                lagPeriode(
                    grunnlag.søknadsperiode,
                    grunnlag.trygdedekning.utenYrkesskadedel(),
                    InnvilgelsesResultat.INNVILGET,
                    grunnlag.bestemmelse
                )
            )
        }

        return lagInnvilgetPeriode(grunnlag)
    }

    private fun lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(grunnlag: UtledMedlemskapsperioderGrunnlag): Collection<Medlemskapsperiode> {
        val søknadsperiode = grunnlag.søknadsperiode

        if (grunnlag.trygdedekning.erKunPensjonsdel()) {
            return lagInnvilgetPeriode(grunnlag)
        }

        if (grunnlag.trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE) {
            return setOf(
                lagPeriode(
                    grunnlag.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_TREDJE_LEDD_YRKESSKADE,
                    InnvilgelsesResultat.AVSLAATT,
                    grunnlag.bestemmelse
                ),
                lagPeriode(
                    grunnlag.søknadsperiode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    InnvilgelsesResultat.INNVILGET,
                    grunnlag.bestemmelse
                )
            )
        }

        if (søknadsperiode.tom != null && søknadsperiode.tom.isBefore(grunnlag.mottaksdatoSøknad)) {
            return lagMedlemskapsperioderForPeriodeFørMottaksdato(søknadsperiode, grunnlag)
        }

        val splittetPeriode = splitPeriode(søknadsperiode, grunnlag.mottaksdatoSøknadNotNull)
        if (grunnlag.trygdedekning in listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE
            )
        ) {
            return lagSplittetYrkesskadeperioder(grunnlag, splittetPeriode)
        }
        return lagMedlemskapsperioderForPeriodeFørMottaksdato(splittetPeriode.first, grunnlag).plus(
            lagPeriode(
                splittetPeriode.second,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.INNVILGET,
                grunnlag.bestemmelse
            )
        )
    }


    private fun lagMedlemskapsperioderForPeriodeFørMottaksdato(
        periode: ErPeriode,
        grunnlag: UtledMedlemskapsperioderGrunnlag
    ): Collection<Medlemskapsperiode> =
        if (grunnlag.trygdedekning.harPensjonsdel()) {
            setOf(
                lagPeriode(
                    periode,
                    grunnlag.trygdedekning.utenPensjonsdel(),
                    InnvilgelsesResultat.AVSLAATT,
                    grunnlag.bestemmelse
                ),
                lagPeriode(
                    periode,
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
                    InnvilgelsesResultat.INNVILGET,
                    grunnlag.bestemmelse
                )
            )
        } else setOf(
            lagPeriode(
                periode,
                grunnlag.trygdedekning,
                InnvilgelsesResultat.AVSLAATT,
                grunnlag.bestemmelse
            )
        )

    private fun lagSplittetYrkesskadeperioder(
        dto: UtledMedlemskapsperioderGrunnlag,
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

