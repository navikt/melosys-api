package no.nav.melosys.service.medlemskapsperiode;


import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283
 */
class UtledMedlemskapsperioderTest {

    private final String arbeidsland = "BR";
    private final Folketrygdloven_kap2_bestemmelser BESTEMMELSE_2_7 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD;
    private final Folketrygdloven_kap2_bestemmelser BESTEMMELSE_2_8 = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A;
    private final LocalDate mottaksdato = LocalDate.now();

    private final UtledMedlemskapsperioder utledMedlemskapsperioder = new UtledMedlemskapsperioder();

    @Test
    void lagMedlemskapsperioder_ukjentBestemmelse_kasterFeil() {
        var request = new UtledMedlemskapsperioderDto(new Periode(mottaksdato, mottaksdato.plusYears(1)),
            Trygdedekninger.FULL_DEKNING_FTRL, mottaksdato, arbeidsland, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utledMedlemskapsperioder.lagMedlemskapsperioder(request))
            .withMessageContaining("Støtter ikke bestemmelse");
    }

    //Scenario 1

    @Test
    void lagMedlemskapsperioder2_8_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHeleSøknadsperiodeMedOppgittTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato, mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperioder2_8_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(20), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    //Scenario 2

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    //Scenario 3

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(1), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter2ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(2), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter3ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirAvslåttMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    //Scenario 4

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrepengerSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter1MndOg1DagFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(1).minusDays(1), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), null);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8)
        );
    }


    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrePenger_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelMedSykeForeldrepengerForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8)
        );
    }

    //Scenario 5

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter2MndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(2), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_8),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    @Test
    void lagMedlemskapsperiode2_8_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_enPeriodeAvslåttEnInnvilget() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_8);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_8)
        );
    }

    // Ny vurdering / Manglende innbetaling

    @Test
    void lagMedlemskapsperioderForAndregangsbehandling_finnesMedlemskapsperioder_filtrererBortAvslåtte() {
        var fom = LocalDate.parse("2023-06-01");
        var tom = LocalDate.parse("2024-06-01");
        var innvilgetMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8);
        var avslåttMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);
        var opphørtMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.OPPHØRT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);

        var opprinneligBehandlingsresultat = new Behandlingsresultat();
        opprinneligBehandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        opprinneligBehandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode, avslåttMedlemskapsperiode, opphørtMedlemskapsperiode));


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, BESTEMMELSE_2_8, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, Behandlingstyper.NY_VURDERING);


        assertThat(response).hasSize(1)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    fom, tom,
                    innvilgetMedlemskapsperiode.getTrygdedekning(), InnvilgelsesResultat.INNVILGET
                )
            );
    }

    @Test
    void lagMedlemskapsperioderForAndregangsbehandling_ingenMedlemskapsperioder_returnererTomListe() {
        var opprinneligBehandlingsresultat = new Behandlingsresultat();
        opprinneligBehandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        opprinneligBehandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of());


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, BESTEMMELSE_2_8, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, Behandlingstyper.NY_VURDERING);


        assertThat(response).isEmpty();
    }

    @Test
    void lagMedlemskapsperioderForAndregangsbehandling_ulovligKombinasjon_oppdatererTrygdedekning() {
        var fom = LocalDate.parse("2023-06-01");
        var tom = LocalDate.parse("2024-06-01");
        var innvilgetMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8);
        var avslåttMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);
        var opphørtMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.OPPHØRT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);

        var opprinneligBehandlingsresultat = new Behandlingsresultat();
        opprinneligBehandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        opprinneligBehandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode, avslåttMedlemskapsperiode, opphørtMedlemskapsperiode));


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, BESTEMMELSE_2_7, Trygdedekninger.FULL_DEKNING_FTRL, Behandlingstyper.NY_VURDERING);


        assertThat(response).hasSize(1)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    fom, tom,
                    Trygdedekninger.FULL_DEKNING_FTRL, InnvilgelsesResultat.INNVILGET
                )
            );
    }

    @Test
    void lagMedlemskapsperioderForAndregangsbehandling_manglendeInnbetaling_filtrererBortAvslåtteMenTarVarePåOpphørte() {
        var fom = LocalDate.parse("2023-06-01");
        var tom = LocalDate.parse("2024-06-01");
        var innvilgetMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8);
        var avslåttMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);
        var opphørtMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.OPPHØRT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);

        var opprinneligBehandlingsresultat = new Behandlingsresultat();
        opprinneligBehandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        opprinneligBehandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode, avslåttMedlemskapsperiode, opphørtMedlemskapsperiode));


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, BESTEMMELSE_2_8, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);


        assertThat(response).hasSize(2)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    fom, tom,
                    innvilgetMedlemskapsperiode.getTrygdedekning(), InnvilgelsesResultat.INNVILGET
                ),
                tuple(
                    fom, tom,
                    opphørtMedlemskapsperiode.getTrygdedekning(), InnvilgelsesResultat.OPPHØRT
                )
            );
    }

    @Test
    void lagMedlemskapsperioderForAndregangsbehandling_bestemmelseErUlik_oppdatererBestemmelsePåIkkeOpphørte() {
        var fom = LocalDate.parse("2023-06-01");
        var tom = LocalDate.parse("2024-06-01");
        var innvilgetMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, BESTEMMELSE_2_8);
        var avslåttMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);
        var opphørtMedlemskapsperiode = lagMedlemskapsperiode(fom, tom, InnvilgelsesResultat.OPPHØRT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, BESTEMMELSE_2_8);

        var annen2_8_Bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var opprinneligBehandlingsresultat = new Behandlingsresultat();
        opprinneligBehandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        opprinneligBehandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode, avslåttMedlemskapsperiode, opphørtMedlemskapsperiode));


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, annen2_8_Bestemmelse, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);


        assertThat(response).hasSize(2)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom, Medlemskapsperiode::getBestemmelse,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    fom, tom, annen2_8_Bestemmelse,
                    innvilgetMedlemskapsperiode.getTrygdedekning(), InnvilgelsesResultat.INNVILGET
                ),
                tuple(
                    fom, tom, BESTEMMELSE_2_8,
                    opphørtMedlemskapsperiode.getTrygdedekning(), InnvilgelsesResultat.OPPHØRT
                )
            );
    }

    // Frivillig medlemskap etter 2-7 eller 2-7a
    // Scenario 1

    @Test
    void lagMedlemskapsperioder2_7_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHeleSøknadsperiodeMedOppgittTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato, mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_7);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_7)
        );
    }

    @Test
    void lagMedlemskapsperiodet2_7_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(20), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_7);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_7)
        );
    }

    // Scenario 2

    @Test
    void lagMedlemskapsperiode2_7_søknadsperiodenStarterFørMottaksdatoMenSlutterEtter_avslåttFørOgInnvilgetEtterPåMottaksdato() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(6), mottaksdato.plusMonths(6));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_7);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_7),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning, BESTEMMELSE_2_7)
        );
    }

    @Test
    void lagMedlemskapsperiode2_7_søknadsperiodenStarterOgSlutterFørMottaksdato_avslåHelePerioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(1), mottaksdato.minusMonths(6));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland, BESTEMMELSE_2_7);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning, BESTEMMELSE_2_7)
        );
    }


    private Medlemskapsperiode lagMedlemskapsperiode(LocalDate fom, LocalDate tom, InnvilgelsesResultat innvilgelsesResultat, Trygdedekninger trygdedekning, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        var forventetMedlemskapsperiode = new Medlemskapsperiode();
        forventetMedlemskapsperiode.setFom(fom);
        forventetMedlemskapsperiode.setTom(tom);
        forventetMedlemskapsperiode.setArbeidsland(arbeidsland);
        forventetMedlemskapsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        forventetMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        forventetMedlemskapsperiode.setTrygdedekning(trygdedekning);
        forventetMedlemskapsperiode.setBestemmelse(bestemmelse);
        return forventetMedlemskapsperiode;
    }
}
