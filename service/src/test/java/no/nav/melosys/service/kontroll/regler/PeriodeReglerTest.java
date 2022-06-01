package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.time.ZoneId;

import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodeReglerTest {

    @Test
    void feilIPeriode_erGyldigPeriode_false() {
        assertThat(PeriodeRegler.feilIPeriode(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(2))).isFalse();
    }

    @Test
    void feilIPeriode_tomFørFom_true() {
        assertThat(PeriodeRegler.feilIPeriode(LocalDate.now().plusYears(1), LocalDate.now())).isTrue();
    }

    @Test
    void periodeErÅpen_ingenTilDato_true() {
        assertThat(PeriodeRegler.periodeErÅpen(LocalDate.now().plusMonths(6), null)).isTrue();
    }

    @Test
    void periodeErÅpen_ikkeÅpen_false() {
        assertThat(PeriodeRegler.periodeErÅpen(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(1))).isFalse();
    }

    @Test
    void periodeOver24Mnd_periodeOver24Mnd_true() {
        assertThat(PeriodeRegler.periodeOver24Måneder(LocalDate.now(), LocalDate.now().plusYears(3))).isTrue();
    }

    @Test
    void periodeoOver24Mnd_periode24Mnd_true() {
        assertThat(PeriodeRegler.periodeOver24Måneder(LocalDate.now(), LocalDate.now().plusMonths(24))).isTrue();
    }

    @Test
    void periodeoOver24Mnd_periode23Mnd_false() {
        assertThat(PeriodeRegler.periodeOver24Måneder(LocalDate.now(), LocalDate.now().plusMonths(23))).isFalse();
    }

    @Test
    void periodeOver24Mnd_periode14Mnd_false() {
        assertThat(PeriodeRegler.periodeOver24Måneder(LocalDate.now(), LocalDate.now().plusMonths(14))).isFalse();
    }

    @Test
    void periodeOver3År_periode2År_false() {
        assertThat(PeriodeRegler.periodeOver3År(LocalDate.now(), LocalDate.now().plusYears(2).plusMonths(11))).isFalse();
    }

    @Test
    void periodeOver3År_periode3År_true() {
        assertThat(PeriodeRegler.periodeOver3År(LocalDate.now(), LocalDate.now().plusYears(3))).isTrue();
    }

    @Test
    void periodeOver3År_periode4År_true() {
        assertThat(PeriodeRegler.periodeOver3År(LocalDate.now(), LocalDate.now().plusYears(4))).isTrue();
    }

    @Test
    void periodeOver5År_periode5År_true() {
        assertThat(PeriodeRegler.periodeOver5År(LocalDate.now(), LocalDate.now().plusYears(5))).isTrue();
    }

    @Test
    void periodeOver5År_periode4År_false() {
        assertThat(PeriodeRegler.periodeOver5År(LocalDate.now(), LocalDate.now().plusYears(4).plusMonths(11))).isFalse();
    }

    @Test
    void datoEldreEnn3År_dato6årSiden_true() {
        assertThat(PeriodeRegler.datoEldreEnn3År(LocalDate.now().minusYears(6))).isTrue();
    }

    @Test
    void datoEldreEnn3År_datoFraNå_false() {
        assertThat(PeriodeRegler.datoEldreEnn3År(LocalDate.now())).isFalse();
    }

    @Test
    void datoEldreEnn2Mnd_datoFraNå_false() {
        assertThat(PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())).isFalse();
    }

    @Test
    void datoEldreEnn2Mnd_datoToMndSiden_false() {
        assertThat(PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant())).isFalse();
    }

    @Test
    void datoEldreEnn2Mnd_datoToMndSidenOgEnDag_true() {
        assertThat(PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())).isTrue();
    }

    @Test
    void datoEldreEnn2Mnd_datoTreMndSiden_true() {
        assertThat(PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(3).atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .isTrue();
    }

    @Test
    void periodeOver1ÅrFremITid_periodeOm2År_true() {
        assertThat(PeriodeRegler.datoOver1ÅrFremITid(LocalDate.now().plusYears(2))).isTrue();
    }

    @Test
    void periodeOver1ÅrFremITid_periodeNå_false() {
        assertThat(PeriodeRegler.datoOver1ÅrFremITid(LocalDate.now())).isFalse();
    }

    @Test
    void periodeErLik_periodeLik_true() {
        assertThat(
            PeriodeRegler.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), LocalDate.now())
        ).isTrue();
    }

    @Test
    void periodeErLik_periodeIkkeLik_false() {
        assertThat(
            PeriodeRegler.periodeErLik(LocalDate.now(), LocalDate.now().plusYears(1), LocalDate.now(), LocalDate.now())
        ).isFalse();
    }

    @Test
    void periodeErLik_tomErNullPeriodeLik_true() {
        assertThat(
            PeriodeRegler.periodeErLik(LocalDate.now(), null, LocalDate.now(), null)
        ).isTrue();
    }

    @Test
    void periodeErLik_tom2ErNullPeriodeIkkeLik_false() {
        assertThat(
            PeriodeRegler.periodeErLik(LocalDate.now(), LocalDate.now(), LocalDate.now(), null)
        ).isFalse();
    }

    @Test
    void datoErFørFørsteJuni2012_datoI2020_false() {
        assertThat(PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2020, 1, 1))).isFalse();
    }

    @Test
    void datoErFørFørsteJuni2012_dato01012012_true() {
        assertThat(PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2012, 1, 1))).isTrue();
    }
}
