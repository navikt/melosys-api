package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodeKontrollerTest {

    @Test
    public void feilIPeriode_erGyldigPeriode_false() {
        assertThat(PeriodeKontroller.feilIPeriode(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(2))).isFalse();
    }

    @Test
    public void feilIPeriode_tomFørFom_true() {
        assertThat(PeriodeKontroller.feilIPeriode(LocalDate.now().plusYears(1), LocalDate.now())).isTrue();
    }

    @Test
    public void periodeErÅpen_ingenTilDato_true() {
        assertThat(PeriodeKontroller.periodeErÅpen(LocalDate.now().plusMonths(6), null)).isTrue();
    }

    @Test
    public void periodeErÅpen_ikkeÅpen_false() {
        assertThat(PeriodeKontroller.periodeErÅpen(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(1))).isFalse();
    }

    @Test
    public void periodeOver24Mnd_periodeOver24Mnd_true() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusYears(3))).isTrue();
    }

    @Test
    public void periodeoOver24Mnd_periode24Mnd_true() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusMonths(24))).isTrue();
    }

    @Test
    public void periodeoOver24Mnd_periode23Mnd_false() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusMonths(23))).isFalse();
    }

    @Test
    public void periodeOver24Mnd_periode14Mnd_false() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusMonths(14))).isFalse();
    }

    @Test
    public void periodeOver5År_periode5År_false() {
        assertThat(PeriodeKontroller.periodeOver5År(LocalDate.now(), LocalDate.now().plusYears(5))).isTrue();
    }

    @Test
    public void periodeOver5År_periode4År_false() {
        assertThat(PeriodeKontroller.periodeOver5År(LocalDate.now(), LocalDate.now().plusYears(4).plusMonths(11))).isFalse();
    }

    @Test
    public void datoEldreEnn3År_dato6årSiden_true() {
        assertThat(PeriodeKontroller.datoEldreEnn3År(LocalDate.now().minusYears(6))).isTrue();
    }

    @Test
    public void datoEldreEnn3År_datoFraNå_false() {
        assertThat(PeriodeKontroller.datoEldreEnn3År(LocalDate.now())).isFalse();
    }

    @Test
    public void datoEldreEnn2Mnd_datoFraNå_false() {
        assertThat(PeriodeKontroller.datoEldreEnn2Mnd(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())).isFalse();
    }

    @Test
    public void datoEldreEnn2Mnd_datoToMndSiden_false() {
        assertThat(PeriodeKontroller.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant())).isFalse();
    }

    @Test
    public void datoEldreEnn2Mnd_datoToMndSidenOgEnDag_true() {
        assertThat(PeriodeKontroller.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())).isTrue();
    }

    @Test
    public void datoEldreEnn2Mnd_datoTreMndSiden_true() {
        assertThat(PeriodeKontroller.datoEldreEnn2Mnd(LocalDate.now().minusMonths(3).atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .isTrue();
    }

    @Test
    public void periodeOver1ÅrFremITid_periodeOm2År_true() {
        assertThat(PeriodeKontroller.datoOver1ÅrFremITid(LocalDate.now().plusYears(2))).isTrue();
    }

    @Test
    public void periodeOver1ÅrFremITid_periodeNå_false() {
        assertThat(PeriodeKontroller.datoOver1ÅrFremITid(LocalDate.now())).isFalse();
    }

    @Test
    public void periodeErLik_periodeLik_true() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), LocalDate.now())
        ).isTrue();
    }

    @Test
    public void periodeErLik_periodeIkkeLik_false() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now().plusYears(1), LocalDate.now(), LocalDate.now())
        ).isFalse();
    }

    @Test
    public void periodeErLik_tomErNullPeriodeLik_true() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), null, LocalDate.now(), null)
        ).isTrue();
    }

    @Test
    public void periodeErLik_tom2ErNullPeriodeIkkeLik_false() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), null)
        ).isFalse();
    }
}