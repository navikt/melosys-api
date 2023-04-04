package no.nav.melosys.service.medlemskapsperiode;


import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283#
 */
class UtledMedlemskapsperioderTest {

    private final String arbeidsland = "BR";
    private final LocalDate mottaksdato = LocalDate.now();

    private final UtledMedlemskapsperioder utledMedlemskapsperioder = new UtledMedlemskapsperioder();
    //Scenario 1

    @Test
    void lagMedlemskapsperioder_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHeleSøknadsperiodeMedOppgittTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato, mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperioder_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(20), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    //Scenario 2

    @Test
    void lagMedlemskapsperiode_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    //Scenario 3

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(1), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter2ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(2), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter3ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirAvslåttMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    //Scenario 4

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL),
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL),
            new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrepengerSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER),
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL),
            new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter1MndOg1DagFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(1).minusDays(1), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL),
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL),
            new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), null);
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL),
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL),
            new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL),
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL)
        );
    }


    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrePenger_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelMedSykeForeldrepengerForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER),
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, Trygdedekninger.PENSJONSDEL)
        );
    }

    //Scenario 5

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter2MndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(2), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSEDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, trygdedekning),
            new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_enPeriodeAvslåttEnInnvilget() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        var request = new UtledMedlemskapsperioderRequest(søknadsPeriode, trygdedekning, bestemmelse, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.PLIKTIG, trygdedekning)
        );
    }
}
