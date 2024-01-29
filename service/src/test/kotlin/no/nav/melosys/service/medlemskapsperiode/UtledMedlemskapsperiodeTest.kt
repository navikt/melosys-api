package no.nav.melosys.service.medlemskapsperiode

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.exception.FunksjonellException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283
 */
internal class UtledMedlemskapsperioderTest {
    private val ARBEIDSLAND = "BR"
    private val BESTEMMELSE_2_7 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD
    private val BESTEMMELSE_2_8 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    private val TRYGDEDEKNING_2_7 = Trygdedekninger.FULL_DEKNING_FTRL
    private val TRYGDEDEKNING_2_8 = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
    private val MOTTAKSDATO = LocalDate.now()

    private val utledMedlemskapsperioder = UtledMedlemskapsperioder()

    @Test
    fun lagMedlemskapsperioder_ukjentBestemmelse_kasterFeil() {
        val request = UtledMedlemskapsperioderDto(
            Periode(MOTTAKSDATO, MOTTAKSDATO.plusYears(1)),
            Trygdedekninger.FULL_DEKNING_FTRL, MOTTAKSDATO, ARBEIDSLAND, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
        )
        Assertions.assertThatExceptionOfType(FunksjonellException::class.java)
            .isThrownBy { utledMedlemskapsperioder.lagMedlemskapsperioder(request) }
            .withMessageContaining("Støtter ikke bestemmelse")
    }

    // Frivillig medlemskap etter 2-8
    // Scenario 1
    @Test
    fun lagMedlemskapsperioder2_8_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHelesøknadsperiodeMedOppgittTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO, MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FULL_DEKNING_FTRL
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            }
    }

    @Test
    fun lagMedlemskapsperioder2_8_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusDays(20), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            }
    }


    // Scenario 2
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(3), MOTTAKSDATO)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }


    // Scenario 3
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(1), MOTTAKSDATO)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter2ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusYears(2), MOTTAKSDATO.minusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
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
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }


    // Scenario 4
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrepengerSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter1MndOg1DagFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(1).minusDays(1), MOTTAKSDATO.plusMonths(4))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), null)
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(3)
            .toList().run {
                get(0).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                get(1).run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                get(2).run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelForsøknadsperioden() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.minusMonths(3))
        val request = UtledMedlemskapsperioderDto(
            søknadsperiode,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
            MOTTAKSDATO,
            ARBEIDSLAND,
            BESTEMMELSE_2_8
        )


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrePenger_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelMedSykeForeldrepengerForsøknadsperioden() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(15), MOTTAKSDATO.minusMonths(3))
        val request = UtledMedlemskapsperioderDto(
            søknadsperiode,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
            MOTTAKSDATO,
            ARBEIDSLAND,
            BESTEMMELSE_2_8
        )


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                last().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }


    // Scenario 5
    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter2MndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(2), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                }
                last().run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    trygdedekning.shouldBe(dekning)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(30), MOTTAKSDATO.minusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }

    @Test
    fun lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_enPeriodeAvslått() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(30), MOTTAKSDATO.plusYears(1))
        val dekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER
        val request = UtledMedlemskapsperioderDto(søknadsperiode, dekning, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_8)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                trygdedekning.shouldBe(dekning)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }


    // Ny vurdering / Manglende innbetaling / Andregangsbehandling
    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_nyVurdering_finnesMedlemskapsperioder_filtrererBortAvslåtte() {
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(
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
            }
        }


        val response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            opprinneligBehandlingsresultat,
            BESTEMMELSE_2_8,
            TRYGDEDEKNING_2_8,
            Behandlingstyper.NY_VURDERING
        )


        opprinneligBehandlingsresultat.medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(3)
        response.shouldHaveSize(1).single().innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_ingenMedlemskapsperioder_returnererTomListe() {
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = emptyList() }
        }


        val response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            opprinneligBehandlingsresultat,
            BESTEMMELSE_2_8,
            TRYGDEDEKNING_2_8,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldNotBeNull().shouldBeEmpty()
    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_ulovligKombinasjon_oppdatererTrygdedekning() {
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(
                    Medlemskapsperiode().apply {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = BESTEMMELSE_2_8
                        trygdedekning = TRYGDEDEKNING_2_8
                    })
            }
        }


        val response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            opprinneligBehandlingsresultat,
            BESTEMMELSE_2_7,
            TRYGDEDEKNING_2_7,
            Behandlingstyper.NY_VURDERING
        )


        response.shouldHaveSize(1)
            .single().trygdedekning.shouldBe(TRYGDEDEKNING_2_7)

    }

    @Test
    fun lagMedlemskapsperioderForAndregangsbehandling_manglendeInnbetaling_filtrererBortAvslåtteMenTarVarePåOpphørte() {
        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(
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
            }
        }


        val response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            opprinneligBehandlingsresultat,
            BESTEMMELSE_2_8,
            TRYGDEDEKNING_2_8,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )


        opprinneligBehandlingsresultat.medlemAvFolketrygden.medlemskapsperioder.shouldHaveSize(3)
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

        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                medlemskapsperioder = listOf(
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
            }
        }


        val response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
            opprinneligBehandlingsresultat,
            nyBestemmelse2_8,
            TRYGDEDEKNING_2_8,
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
        val request = UtledMedlemskapsperioderDto(søknadsperiode, TRYGDEDEKNING_2_7, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_7)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                bestemmelse.shouldBe(BESTEMMELSE_2_7)
            }
    }

    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusDays(20), MOTTAKSDATO.plusYears(1))
        val request = UtledMedlemskapsperioderDto(søknadsperiode, TRYGDEDEKNING_2_7, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_7)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                fom.shouldBe(søknadsperiode.fom)
                tom.shouldBe(søknadsperiode.tom)
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                bestemmelse.shouldBe(BESTEMMELSE_2_7)
            }
    }


    // Scenario 2
    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodenStarterFørMottaksdatoMenSlutterEtter_avslåttFørOgInnvilgetEtterPåMottaksdato() {
        val søknadsperiode = Periode(MOTTAKSDATO.minusMonths(6), MOTTAKSDATO.plusMonths(6))
        val request = UtledMedlemskapsperioderDto(søknadsperiode, TRYGDEDEKNING_2_7, MOTTAKSDATO, ARBEIDSLAND, BESTEMMELSE_2_7)


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(2)
            .run {
                first().run {
                    fom.shouldBe(søknadsperiode.fom)
                    tom.shouldBe(MOTTAKSDATO.minusDays(1))
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                    bestemmelse.shouldBe(BESTEMMELSE_2_7)
                }
                last().run {
                    fom.shouldBe(MOTTAKSDATO)
                    tom.shouldBe(søknadsperiode.tom)
                    innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    trygdedekning.shouldBe(TRYGDEDEKNING_2_7)
                    bestemmelse.shouldBe(BESTEMMELSE_2_7)
                }
            }
    }

    @Test
    fun lagMedlemskapsperioder2_7_søknadsperiodenStarterOgSlutterFørMottaksdato_avslåHelePerioden() {
        val request = UtledMedlemskapsperioderDto(
            Periode(MOTTAKSDATO.minusYears(1), MOTTAKSDATO.minusMonths(6)),
            TRYGDEDEKNING_2_7,
            MOTTAKSDATO,
            ARBEIDSLAND,
            BESTEMMELSE_2_7
        )


        utledMedlemskapsperioder.lagMedlemskapsperioder(request)
            .shouldHaveSize(1)
            .single().run {
                innvilgelsesresultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
            }
    }
}
