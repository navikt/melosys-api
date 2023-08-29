package no.nav.melosys.service.medlemskapsperiode;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Scenarioer definert på https://confluence.adeo.no/pages/viewpage.action?pageId=387109283
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
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperioder_søknadsperiodeStarter20DagerFørMottaksdato_helePeriodenBlirInnvilgetMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusDays(20), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    //Scenario 2

    @Test
    void lagMedlemskapsperiode_søknadsperiodeStarterMerEnnToÅrFørMottaksDato_helePeriodenBlirAvslåttMedSøktTrygdedekning() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning)
        );
    }

    //Scenario 3

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter1ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(1), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter2ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirInnvilgetMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(2), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter3ÅrFørMottaksdatoTrygdedekningErPensjonsdel_helePeriodenBlirAvslåttMedPensjonsdel() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusYears(3), mottaksdato);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning)
        );
    }

    //Scenario 4

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrepengerSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter1MndOg1DagFørMottaksdatoMedHelseOgPensjonsdelSlutter4MndEtterMottaksdato_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(1).minusDays(1), mottaksdato.plusMonths(4));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoUtenSluttdatoMedHelseOgPensjonsdel_enAvslåttOgInnvilgetMedSammePeriodeOgEnInnvilgetPeriode() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), null);
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdel_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
        );
    }


    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter15MndFørMottaksdatoSluttdato3MndFørMottaksdatoMedHelseOgPensjonsdelMedSykeOgForeldrePenger_avslåttOriginalDekningMenInnvilgetMedKunPensjonsdelMedSykeForeldrepengerForSøknadsperioden() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(15), mottaksdato.minusMonths(3));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER),
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON)
        );
    }

    //Scenario 5

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter2MndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_toPerioderAvslåttFremTilMottaksdato() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(2), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), mottaksdato.minusDays(1), InnvilgelsesResultat.AVSLAATT, trygdedekning),
            lagMedlemskapsperiode(mottaksdato, søknadsPeriode.getTom(), InnvilgelsesResultat.INNVILGET, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrFørMottaksdatoMedHelsedel_enPeriodeAvslått() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.minusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning)
        );
    }

    @Test
    void lagMedlemskapsperiode_søknadsperiodenStarter30mndFørMottaksdatoSluttdato1ÅrEtterMottaksdatoMedHelsedel_enPeriodeAvslåttEnInnvilget() {
        final Periode søknadsPeriode = new Periode(mottaksdato.minusMonths(30), mottaksdato.plusYears(1));
        final Trygdedekninger trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER;
        var request = new UtledMedlemskapsperioderDto(søknadsPeriode, trygdedekning, mottaksdato, arbeidsland);

        assertThat(
            utledMedlemskapsperioder.lagMedlemskapsperioder(request)
        ).containsOnly(
            lagMedlemskapsperiode(søknadsPeriode.getFom(), søknadsPeriode.getTom(), InnvilgelsesResultat.AVSLAATT, trygdedekning)
        );
    }

    // Ny vurdering
    // Eksempel 1

    @Test
    void lagMedlemskapsperioderForNyVurdering_leggerTilSykeOgForeldrepengerEndrerSøknadsperiode_innvilgerDekningEndringFremITidOgBrukerOriginaleReglerPåUtvidetPeriode() {
        var opprinneligPeriode = new Periode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"));
        var opprinneligTrygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var opprinneligMedlemskapsperiode = lagMedlemskapsperiode(
            opprinneligPeriode.getFom(), opprinneligPeriode.getTom(), InnvilgelsesResultat.INNVILGET, opprinneligTrygdedekning);
        var opprinneligSøknad = new SøknadNorgeEllerUtenforEØS();
        opprinneligSøknad.setTrygdedekning(opprinneligTrygdedekning);
        opprinneligSøknad.periode = opprinneligPeriode;
        opprinneligSøknad.soeknadsland = new Soeknadsland(List.of(arbeidsland), false);

        var søknadsPeriode = new Periode(LocalDate.parse("2022-12-15"), LocalDate.parse("2023-12-15"));
        var trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER;
        var nyMottaksdato = LocalDate.parse("2023-02-15");

        var request = new UtledMedlemskapsperiodeNyVurderingDto(
            søknadsPeriode, trygdedekning, nyMottaksdato, arbeidsland, List.of(opprinneligMedlemskapsperiode), opprinneligSøknad);

        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForNyVurdering(request);

        assertThat(response).hasSize(4)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    søknadsPeriode.getFom(), opprinneligPeriode.getFom().minusDays(1),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER, InnvilgelsesResultat.AVSLAATT
                ),
                tuple(
                    søknadsPeriode.getFom(), opprinneligPeriode.getFom().minusDays(1),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.INNVILGET
                ),
                tuple(
                    opprinneligPeriode.getFom(), nyMottaksdato.minusDays(1),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, InnvilgelsesResultat.INNVILGET
                ),
                tuple(
                    nyMottaksdato, søknadsPeriode.getTom(),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER, InnvilgelsesResultat.INNVILGET
                )
            );

    }

    // Ny vurdering
    // Eksempel 2

    @Test
    void lagMedlemskapsperioderForNyVurdering_leggerTilPensjonsdelUtviderPeriode_pensjonsdelLagtTilBakITidPeriodeForlengetMenHelsedelAvslåttBakITid() {
        var opprinneligPeriode = new Periode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"));
        var opprinneligTrygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE;
        var opprinneligMedlemskapsperiode = lagMedlemskapsperiode(
            opprinneligPeriode.getFom(), opprinneligPeriode.getTom(), InnvilgelsesResultat.INNVILGET, opprinneligTrygdedekning);
        var opprinneligSøknad = new SøknadNorgeEllerUtenforEØS();
        opprinneligSøknad.setTrygdedekning(opprinneligTrygdedekning);
        opprinneligSøknad.periode = opprinneligPeriode;
        opprinneligSøknad.soeknadsland = new Soeknadsland(List.of(arbeidsland), false);

        var søknadsPeriode = new Periode(LocalDate.parse("2023-01-01"), LocalDate.parse("2024-03-01"));
        var trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var nyMottaksdato = LocalDate.parse("2024-04-15");

        var request = new UtledMedlemskapsperiodeNyVurderingDto(
            søknadsPeriode, trygdedekning, nyMottaksdato, arbeidsland, List.of(opprinneligMedlemskapsperiode), opprinneligSøknad);

        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForNyVurdering(request);

        assertThat(response)
            .hasSize(3)
            .extracting(
                Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getTrygdedekning, Medlemskapsperiode::getInnvilgelsesresultat
            )
            .containsExactlyInAnyOrder(
                tuple(
                    opprinneligPeriode.getFom(), opprinneligPeriode.getTom(),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, InnvilgelsesResultat.INNVILGET
                ),
                tuple(
                    opprinneligPeriode.getTom().plusDays(1), søknadsPeriode.getTom(),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, InnvilgelsesResultat.AVSLAATT
                ),
                tuple(
                    opprinneligPeriode.getTom().plusDays(1), søknadsPeriode.getTom(),
                    Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON, InnvilgelsesResultat.INNVILGET
                )
            );
    }

    @Test
    void lagMedlemskapsperioderForNyVurdering_ingenOpprinneligeMedlemskapsperioder_lagerFørstegangsMedlemskapsperioder() {
        var opprinneligSøknad = new SøknadNorgeEllerUtenforEØS();

        var søknadsPeriode = new Periode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"));
        var trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON;
        var nyMottaksdato = LocalDate.parse("2023-01-01");

        var request = new UtledMedlemskapsperiodeNyVurderingDto(
            søknadsPeriode, trygdedekning, nyMottaksdato, arbeidsland, Collections.emptyList(), opprinneligSøknad);


        Collection<Medlemskapsperiode> response = utledMedlemskapsperioder.lagMedlemskapsperioderForNyVurdering(request);


        assertThat(response).isNotEmpty();
    }

    private Medlemskapsperiode lagMedlemskapsperiode(LocalDate fom, LocalDate tom, InnvilgelsesResultat innvilgelsesResultat, Trygdedekninger trygdedekning) {
        var forventetMedlemskapsperiode = new Medlemskapsperiode();
        forventetMedlemskapsperiode.setFom(fom);
        forventetMedlemskapsperiode.setTom(tom);
        forventetMedlemskapsperiode.setArbeidsland(arbeidsland);
        forventetMedlemskapsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        forventetMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        forventetMedlemskapsperiode.setTrygdedekning(trygdedekning);
        return forventetMedlemskapsperiode;
    }
}
