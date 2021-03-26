package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisteropplysningerPeriodeFactoryTest {

    private final Integer arbeidsforholdhistorikkAntallMåneder = 6;
    private final Integer medlemskaphistorikkAntallÅr = 5;
    private final Integer inntektshistorikkAntallMåneder = 6;
    private final Behandling behandlingAvSøknad = lagBehandling(true);
    private final Behandling mottakAvSed = lagBehandling(false);

    private RegisteropplysningerPeriodeFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RegisteropplysningerPeriodeFactory(
            arbeidsforholdhistorikkAntallMåneder,
            medlemskaphistorikkAntallÅr,
            inntektshistorikkAntallMåneder);
    }

    @Test
    void hentPeriodeForArbeidsforhold() {
        LocalDate fom = LocalDate.now().minusMonths(1);
        LocalDate tom = LocalDate.now();

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForArbeidsforhold(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(fom.minusMonths(arbeidsforholdhistorikkAntallMåneder));
        assertThat(periode.tom).isEqualTo(LocalDate.now());
    }

    @Test
    void hentPeriodeForArbeidsforhold_fremtidigPeriode() {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForArbeidsforhold(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(LocalDate.now().minusMonths(arbeidsforholdhistorikkAntallMåneder));
        assertThat(periode.tom).isEqualTo(LocalDate.now());
    }

    @Test
    void hentPeriodeForArbeidsforhold_åpenPeriodeMottakSed() {
        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForArbeidsforhold(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(fom.minusMonths(arbeidsforholdhistorikkAntallMåneder));
        assertThat(periode.tom).isEqualTo(LocalDate.now());
    }

    @Test
    void hentPeriodeForArbeidsforhold_åpenPeriodeBehandlingSøknad() {
        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForArbeidsforhold(fom, tom, behandlingAvSøknad);

        assertThat(periode.fom).isEqualTo(fom.minusMonths(arbeidsforholdhistorikkAntallMåneder));
        assertThat(periode.tom).isEqualTo(fom.plusYears(1));
    }

    @Test
    void hentPeriodeForMedlemskap_åpenPeriodeBehandlingSøknad() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForMedlemskap(fom, tom, behandlingAvSøknad);

        assertThat(periode.fom).isEqualTo(fom.minusYears(medlemskaphistorikkAntallÅr));
        assertThat(periode.tom).isEqualTo(fom.plusYears(1));
    }

    @Test
    void hentPeriodeForMedlemskap_åpenPeriodeMottakSed() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.DatoPeriode periode = factory.hentPeriodeForMedlemskap(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(fom.minusYears(medlemskaphistorikkAntallÅr));
        assertThat(periode.tom).isNull();
    }

    @Test
    void hentPeriodeForYtelser_periodePåbegynt_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(YearMonth.from(fom.minusMonths(2)));
        assertThat(periode.tom).isEqualTo(YearMonth.from(LocalDate.now()));
    }

    @Test
    void hentPeriodeForYtelser_periodeIkkePåbegyntMottakSed_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(YearMonth.from(LocalDate.now().minusMonths(2)));
        assertThat(periode.tom).isEqualTo(YearMonth.from(LocalDate.now()));
    }

    @Test
    void hentPeriodeForYtelser_åpenPeriodeIkkePåbegyntMottakSed_verifiserInntektPeriode() {
        LocalDate now = LocalDate.now();
        LocalDate fom = now.plusYears(1);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(YearMonth.from(now.minusMonths(2)));
        assertThat(periode.tom).isEqualTo(YearMonth.from(now));
    }

    @Test
    void hentPeriodeForYtelser_periodeIkkePåbegyntBehandlingSøknad_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, behandlingAvSøknad);

        assertThat(periode.fom).isEqualTo(YearMonth.now().minusMonths(inntektshistorikkAntallMåneder));
        assertThat(periode.tom).isEqualTo(YearMonth.now());
    }

    @Test
    void hentPeriodeForYtelser_periodeAvsluttet_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().minusYears(3);
        LocalDate tom = LocalDate.now().minusYears(2);

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(YearMonth.from(fom));
        assertThat(periode.tom).isEqualTo(YearMonth.from(tom));
    }

    @Test
    void hentPeriodeForYtelser_åpenPeriodeMottakSed_forespørTomTilDato() {
        LocalDate now = LocalDate.now();
        LocalDate fom = now.minusYears(2);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed);

        assertThat(periode.fom).isEqualTo(YearMonth.from(fom.minusMonths(2)));
        assertThat(periode.tom).isEqualTo(YearMonth.from(now));
    }

    @Test
    void hentPeriodeForYtelser_åpenPeriodeBehandlingSøknad_forespørTomTilDato() {
        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = null;

        RegisteropplysningerPeriodeFactory.Periode periode = factory.hentPeriodeForInntekt(fom, tom, behandlingAvSøknad);

        assertThat(periode.fom).isEqualTo(YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder)));
        assertThat(periode.tom).isEqualTo(YearMonth.from(fom.plusYears(1)));
    }

    private Behandling lagBehandling(boolean erBehandlingAvSøknad) {
        Behandling behandling = new Behandling();
        behandling.setTema(erBehandlingAvSøknad
            ? Behandlingstema.UTSENDT_ARBEIDSTAKER
            : Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        return behandling;
    }
}