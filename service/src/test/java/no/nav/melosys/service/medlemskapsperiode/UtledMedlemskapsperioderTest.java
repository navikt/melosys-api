package no.nav.melosys.service.medlemskapsperiode;


import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283#
 */
class UtledMedlemskapsperioderTest {

    //Scenario 1

    @Test
    void lagMedlemskapsperioder_søknadsperiodeStarterPåMottaksdato_genererMedlemskapsperiodeForHeleSøknadsperiodeMedOppgittTrygdedekning() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato, LocalDate.now().plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";


        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    @Test
    void lagMedlemskapsperioder_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(20), LocalDate.now().plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    //Scenario 2

    @Test
    void lagMedlemskapsperiode_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), LocalDate.now());
        final Trygdedekninger trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    //Scenario 3

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(1), LocalDate.now());
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter3ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirAvslåttMedPensjonsdel() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), LocalDate.now());
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    //Scenario 4

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enInnvilgetOgEnDelvisInnvilgetPeriode() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), LocalDate.now().plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.DELVIS_INNVILGET, Medlemskapstyper.FRIVILLIG, Trygdedekninger.PENSJONSDEL),
                new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enInnvilgetOgEnDelvisInnvilgetPeriode() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), null);
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.DELVIS_INNVILGET, Medlemskapstyper.FRIVILLIG, Trygdedekninger.PENSJONSDEL),
                new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_delvisInnvilgetForsøknadsperiodenMedKunPensjonsdel() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.PENSJONSDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.DELVIS_INNVILGET, Medlemskapstyper.FRIVILLIG, Trygdedekninger.PENSJONSDEL)
            );
    }

    //Scenario 5

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15DagerFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSEDEL;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.FRIVILLIG, trygdedekning),
                new Medlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.INNVILGET, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter2ÅrFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        final LocalDate mottaksdato = LocalDate.now();
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(2), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER;
        final Folketrygdloven_kap2_bestemmelser bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
        final String arbeidsland = "BR";

        assertThat(UtledMedlemskapsperioder.lagMedlemskapsperioder(søknadsPeriode, trygdedekning, mottaksdato, bestemmelse, arbeidsland))
            .containsOnly(
                new Medlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), arbeidsland, bestemmelse, InnvilgelsesResultat.AVSLAATT, Medlemskapstyper.FRIVILLIG, trygdedekning)
            );
    }
}