package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283
 */
internal class UtledMedlemskapsperioderTest {
    private val BESTEMMELSE_2_7 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD
    private val BESTEMMELSE_2_8 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    private val BESTEMMELSE_PLIKTIG = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
    private val TRYGDEDEKNING_2_7 = Trygdedekninger.FULL_DEKNING_FTRL
    private val TRYGDEDEKNING_2_8 = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
    private val MOTTAKSDATO = LocalDate.now()
    private val START_AV_ÅRET = LocalDate.of(2025, 1, 1)
    private val SLUTT_AV_ÅRET = LocalDate.of(2025, 12, 31)

    companion object{
        @JvmStatic
        fun SLUTT_AV_ÅRET_og_UKJENT_SLUTTDATO(): Stream<Arguments> = Stream.of(
            Arguments.of(LocalDate.of(2025, 12, 31)),
            Arguments.of(null)
        )
    }


    @Test
    fun lagMedlemskapsperioder_ukjentBestemmelse_kasterFeil() {
        shouldThrow<FunksjonellException> {
            UtledMedlemskapsperioder.lagMedlemskapsperioder(
                UtledMedlemskapsperioderGrunnlag(
                    Medlemskapsperiode(),
                    TRYGDEDEKNING_2_8,
                    mottaksdatoSøknad = null,
                    Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                )
            )
        }.message.shouldContain("Støtter ikke bestemmelse")
    }

    // Pliktig medlemskap
    @Test
    fun lagMedlemskapsperioder_pliktig_innvilgerHelePeriodenMedFullDekningUavhengigAvSøktDekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(3), MOTTAKSDATO.plusYears(3))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_PLIKTIG)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FULL_DEKNING_FTRL)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
            }
    }

    // Frivillig medlemskap etter 2-8
    // Scenario 1
    @Test
    fun lagMedlemskapsperioder2_8_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHelesøknadsperiodeMedOppgittTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO, MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperioder2_8_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusDays(20), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }


    // Scenario 2
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(3), MOTTAKSDATO)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }


    // Scenario 3
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(1), MOTTAKSDATO)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter2ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(2), MOTTAKSDATO.minusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter3ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirAvslåttMedPensjonsdel() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(3), MOTTAKSDATO)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }


    // Scenario 4
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrepengerSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter1MndOg1DagFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(1).minusDays(1), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), null)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelForsøknadsperioden() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.minusMonths(3))
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
            MOTTAKSDATO,
            BESTEMMELSE_2_8
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }
    // Scenarioer på confluence 2_7: https://confluence.adeo.no/pages/viewpage.action?pageId=387109283#Foresl%C3%A5ttemedlemskapsperioderistegvelger-Pensjonist/uf%C3%B8retrygdet-beskrivelseavlogikkvedf%C3%B8rstegangsbehandling.1
    @Test
    fun lagMedlemskapsperiodePensjonist2_7_scenario1_lik_mottaksdato_som_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = START_AV_ÅRET
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FULL_DEKNING,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_7_scenario1_mottaksdato_før_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2024, 12, 15)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FULL_DEKNING,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_7_scenario1_mottaksdato_mindre_enn_en_måned_etter_startdato() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2025, 1, 28)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @ParameterizedTest
    @MethodSource("SLUTT_AV_ÅRET_og_UKJENT_SLUTTDATO")
    fun lagMedlemskapsperiodePensjonist2_7_scenario2_søknadsperioden_starter_mer_enn_en_måned_før_og_sluttdato_er_etter_mottaksdato(TIL_OG_MED: LocalDate?) {
        val søknadsperiode = Periode(START_AV_ÅRET, TIL_OG_MED)
        val mottaksDato = LocalDate.of(2025, 4, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(mottaksDato.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(mottaksDato)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @ParameterizedTest
    @MethodSource("SLUTT_AV_ÅRET_og_UKJENT_SLUTTDATO")
    fun lagMedlemskapsperiodePensjonist2_7_scenario3_søknadsperioden_starter_mer_enn_en_måned_før_og_sluttdato_er_etter_mottaksdato(TIL_OG_MED: LocalDate?) {
        val søknadsperiode = Periode(START_AV_ÅRET, TIL_OG_MED)
        val mottaksDato = LocalDate.of(2025, 4, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FULL_DEKNING_FTRL,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FULL_DEKNING_FTRL)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(mottaksDato)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_7_scenario4_søknadsperioden_starter_mer_enn_en_måned_før_og_sluttdato_er_før_mottaksdato() {
        val søknadsperiode = Periode(START_AV_ÅRET, LocalDate.of(2025, 3, 1))
        val mottaksDato = LocalDate.of(2025, 4, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FULL_DEKNING_FTRL,
            mottaksDato,
            BESTEMMELSE_2_7,
            Behandlingstema.PENSJONIST
        )

        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FULL_DEKNING_FTRL)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    // Scenarioer på confluence 2_8: https://confluence.adeo.no/pages/viewpage.action?pageId=387109283#Foresl%C3%A5ttemedlemskapsperioderistegvelger-Pensjonist/uf%C3%B8retrygdet-beskrivelseavlogikkvedf%C3%B8rstegangsbehandling
    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario1_lik_mottaksdato_som_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = START_AV_ÅRET
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario1_mottaksdato_før_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2024, 12, 15)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario1_mottaksdato_mindre_enn_en_måned_etter_startdato() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2025, 1, 28)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario2_lik_mottaksdato_som_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = START_AV_ÅRET
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario2_mottaksdato_før_søknadsperiode_fom() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2024, 12, 15)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario2_mottaksdato_mindre_enn_en_måned_etter_startdato() {
        val søknadsperiode = Periode(START_AV_ÅRET, SLUTT_AV_ÅRET)
        val mottaksDato = LocalDate.of(2025, 1, 28)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @ParameterizedTest
    @MethodSource("SLUTT_AV_ÅRET_og_UKJENT_SLUTTDATO")
    fun lagMedlemskapsperiodePensjonist2_8_scenario3_Søknadsperioden_starter_mer_enn_en_måned_før_mottaksdato(TIL_OG_MED: LocalDate?) {
        val søknadsperiode = Periode(START_AV_ÅRET, TIL_OG_MED)
        val mottaksDato = LocalDate.of(2025, 3, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(mottaksDato.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(mottaksDato)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }


    @ParameterizedTest
    @MethodSource("SLUTT_AV_ÅRET_og_UKJENT_SLUTTDATO")
    fun lagMedlemskapsperiodePensjonist2_8_scenario4_Søknadsperioden_starter_mer_enn_en_måned_før_mottaksdato(TIL_OG_MED: LocalDate?) {
        val søknadsperiode = Periode(START_AV_ÅRET, TIL_OG_MED)
        val mottaksDato = LocalDate.of(2025, 3, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )

        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(mottaksDato.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(1).run {
                    fom.shouldBe(mottaksDato)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                get(2).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiodePensjonist2_8_scenario5_Søknadsperioden_starter_mer_enn_en_måned_før_mottaksdato_og_sluttdato_er_før_mottaksdato() {
        val søknadsperiode = Periode(START_AV_ÅRET, LocalDate.of(2025, 2, 28))
        val mottaksDato = LocalDate.of(2025, 3, 1)
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
            mottaksDato,
            BESTEMMELSE_2_8,
            Behandlingstema.PENSJONIST
        )

        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)

            }
    }


    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrePenger_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelMedSykeForeldrepengerForsøknadsperioden() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.minusMonths(3))
        val request = UtledMedlemskapsperioderGrunnlag(
            søknadsperiode.hentErPeriode(),
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            MOTTAKSDATO,
            BESTEMMELSE_2_8
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }


    // Scenario 5
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter2MndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(2), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(30), MOTTAKSDATO.minusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_enPeriodeAvslått() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(30), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), dekning, MOTTAKSDATO, BESTEMMELSE_2_8)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }


    // Ny vurdering / Manglende innbetaling / Andregangsbehandling
    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_nyVurdering_finnesMedlemskapsperioder_filtrererBortAvslåtte() {
        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                trygdedekning = TRYGDEDEKNING_2_8
            },
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                trygdedekning = TRYGDEDEKNING_2_8
            },
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                trygdedekning = TRYGDEDEKNING_2_8
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, BESTEMMELSE_2_8),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.NY_VURDERING
        )


        opprinneligeMedlemskapsperioder.shouldHaveSize(3)
        response.shouldHaveSize(1).single().innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_ingenMedlemskapsperioder_returnererTomListe() {
        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, BESTEMMELSE_2_8),
            emptyList(),
            Behandlingstyper.NY_VURDERING
        )


        response.shouldNotBeNull().shouldBeEmpty()
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_ulovligKombinasjon_oppdatererTrygdedekning() {
        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                bestemmelse = BESTEMMELSE_2_8
                trygdedekning = TRYGDEDEKNING_2_8
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_7, null, BESTEMMELSE_2_7),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldHaveSize(1)
            .single().trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_nato_tilleggsavtale() {
        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                bestemmelse = BESTEMMELSE_2_8
                trygdedekning = TRYGDEDEKNING_2_8
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldHaveSize(1)
            .single().trygdedekning.shouldBe(Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL)
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_fraFrivilligTilPliktig_lagerForslagPåPerioderPåNytt() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(12), MOTTAKSDATO.minusMonths(6))
        val nySøknadsperiode = Periode(MOTTAKSDATO.minusMonths(8), MOTTAKSDATO.minusMonths(2))

        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                fom = søknadsperiode.fom!!
                tom = søknadsperiode.tom
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                bestemmelse = BESTEMMELSE_2_8
                trygdedekning = TRYGDEDEKNING_2_8
                medlemskapstype = Medlemskapstyper.FRIVILLIG
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(nySøknadsperiode.hentErPeriode(), TRYGDEDEKNING_2_8, null, BESTEMMELSE_PLIKTIG),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldHaveSize(1)
            .single().run {
                fom.shouldBe(nySøknadsperiode.fom)
                tom.shouldBe(nySøknadsperiode.tom)
                bestemmelse.shouldBe(BESTEMMELSE_PLIKTIG)
                trygdedekning.shouldBe(Trygdedekninger.FULL_DEKNING_FTRL)
                medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
            }
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_fraPliktigTilFrivillig_oppdatererFelt() {
        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                bestemmelse = BESTEMMELSE_PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                medlemskapstype = Medlemskapstyper.PLIKTIG
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, BESTEMMELSE_2_8),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldHaveSize(1)
            .single().run {
                bestemmelse.shouldBe(BESTEMMELSE_2_8)
                trygdedekning.shouldBe(TRYGDEDEKNING_2_8)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_manglendeInnbetaling_filtrererBortAvslåtteMenTarVarePåOpphørte() {
        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                trygdedekning = TRYGDEDEKNING_2_8
            },
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                trygdedekning = TRYGDEDEKNING_2_8
            },
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                trygdedekning = TRYGDEDEKNING_2_8
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, BESTEMMELSE_2_8),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )


        opprinneligeMedlemskapsperioder.shouldHaveSize(3)
        response.shouldHaveSize(2)
            .run {
                first().innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                last().innvilgelsesresultat.shouldBe(InnvilgelsesResultat.OPPHØRT)
            }
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_bestemmelseErUlik_oppdatererBestemmelsePåIkkeOpphørte() {
        val nyBestemmelse2_8 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD
        val opphørtBestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD

        val opprinneligeMedlemskapsperioder = listOf(
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                bestemmelse = BESTEMMELSE_2_8
                trygdedekning = TRYGDEDEKNING_2_8
            },
            Medlemskapsperiode().apply {
                innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                bestemmelse = opphørtBestemmelse
                trygdedekning = TRYGDEDEKNING_2_8
            })


        val response = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            UtledMedlemskapsperioderGrunnlag(object : ErPeriode { override var fom = START_AV_ÅRET; override var tom = SLUTT_AV_ÅRET }, TRYGDEDEKNING_2_8, null, nyBestemmelse2_8),
            opprinneligeMedlemskapsperioder,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )


        response.shouldHaveSize(2).run {
            first().run {
                bestemmelse.shouldBe(nyBestemmelse2_8)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            }
            last().run {
                bestemmelse.shouldBe(opphørtBestemmelse)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.OPPHØRT)
            }
        }
    }


    // Frivillig medlemskap etter 2-7 eller 2-7a
    // Scenario 1
    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHelesøknadsperiodeMedOppgittTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO, MOTTAKSDATO.plusYears(1))
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), TRYGDEDEKNING_2_7, MOTTAKSDATO, BESTEMMELSE_2_7)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                bestemmelse.shouldBe(BESTEMMELSE_2_7)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }

    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusDays(20), MOTTAKSDATO.plusYears(1))
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), TRYGDEDEKNING_2_7, MOTTAKSDATO, BESTEMMELSE_2_7)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                bestemmelse.shouldBe(BESTEMMELSE_2_7)
                medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
            }
    }


    // Scenario 2
    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodenStarterFørMottaksdatoMenSlutterEtter_avslåttFørOgInnvilgetEtterPåMottaksdato() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(6), MOTTAKSDATO.plusMonths(6))
        val request = UtledMedlemskapsperioderGrunnlag(søknadsperiode.hentErPeriode(), TRYGDEDEKNING_2_7, MOTTAKSDATO, BESTEMMELSE_2_7)


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                    bestemmelse.shouldBe(BESTEMMELSE_2_7)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
                last().run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                    bestemmelse.shouldBe(BESTEMMELSE_2_7)
                    medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG)
                }
            }
    }

    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodenStarterOgSlutterFørMottaksdato_avslåHelePerioden() {
        val request = UtledMedlemskapsperioderGrunnlag(
            Periode(MOTTAKSDATO.minusYears(1), MOTTAKSDATO.minusMonths(6)).hentErPeriode(),
            TRYGDEDEKNING_2_7,
            MOTTAKSDATO,
            BESTEMMELSE_2_7
        )


        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }

    @Test
    fun `lagMedlemskapsperioder tilleggsavtale med Nato skal gi dekning Nato Helsedel med innvilget`() {
        val request = UtledMedlemskapsperioderGrunnlag(
            Periode(MOTTAKSDATO.minusYears(1), MOTTAKSDATO.minusMonths(6)).hentErPeriode(),
            Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL,
            MOTTAKSDATO,
            Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO
        )

        UtledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                trygdedekning.shouldBe(Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL)

            }
    }
}
