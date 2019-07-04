package no.nav.melosys.service.kontroll;

import java.time.LocalDate;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodeKontrollerTest {

    @Test
    public void feilIPeriode_erGyldigPeriode_ingenregistrerTreff() {
        assertThat(PeriodeKontroller.feilIPeriode(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(2))).isFalse();
    }

    @Test
    public void feilIPeriode_tomFørFom_registrerTreff() {
        assertThat(PeriodeKontroller.feilIPeriode(LocalDate.now().plusYears(1), LocalDate.now())).isTrue();
    }

    @Test
    public void periodeErÅpen_ingenTilDato_registrerTreff() {
        assertThat(PeriodeKontroller.periodeErÅpen(LocalDate.now().plusMonths(6), null)).isTrue();
    }
    
    @Test
    public void periodeErÅpen_ikkeÅpen_registrerTreff() {
        assertThat(PeriodeKontroller.periodeErÅpen(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(1))).isFalse();
    }

    @Test
    public void periodeOver24Mnd_periodeOver24Mnd_registrerTreff() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusYears(3))).isTrue();
    }
    
    @Test
    public void periodeOver24Mnd_periode14Mnd_registrerTreff() {
        assertThat(PeriodeKontroller.periodeOver24Mnd(LocalDate.now(), LocalDate.now().plusMonths(14))).isFalse();
    }

    @Test
    public void periodeEldreEnn5År_periodeEldreEnn5År_registrerTreff() {
        assertThat(PeriodeKontroller.datoEldreEnn5År(LocalDate.now().minusYears(6))).isTrue();
    }
    
    @Test
    public void periodeEldreEnn5År_periodeFraNå_registrerTreff() {
        assertThat(PeriodeKontroller.datoEldreEnn5År(LocalDate.now())).isFalse();
    }
    
    @Test
    public void periodeOver1ÅrFremITid_periodeOm2År_registrerTreff() {
        assertThat(PeriodeKontroller.datoOver1ÅrFremITid(LocalDate.now().plusYears(2))).isTrue();
    }

    @Test
    public void periodeOver1ÅrFremITid_periodeNå_ingenTreff() {
        assertThat(PeriodeKontroller.datoOver1ÅrFremITid(LocalDate.now())).isFalse();
    }

    @Test
    public void periodeErLik_periodeLik_ingenTreff() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), LocalDate.now())
        ).isTrue();
    }

    @Test
    public void periodeErLik_periodeIkkeLik_ingenTreff() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now().plusYears(1), LocalDate.now(), LocalDate.now())
        ).isFalse();
    }

    @Test
    public void periodeErLik_periodeIkkeLik_registrerTreff() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now().plusYears(1), LocalDate.now(), LocalDate.now())
        ).isFalse();
    }

    @Test
    public void periodeErLik_tomErNullPeriodeLik_registrerTreff() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), null, LocalDate.now(), null)
        ).isTrue();
    }

    @Test
    public void periodeErLik_tom2ErNullPeriodeIkkeLik_registrerTreff() {
        assertThat(
            PeriodeKontroller.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), null)
        ).isFalse();
    }
}