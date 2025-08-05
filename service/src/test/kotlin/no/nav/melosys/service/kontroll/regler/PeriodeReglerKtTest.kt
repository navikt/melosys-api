package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodeReglerKtTest {

    companion object {
        private val DATO = LocalDate.parse("2024-01-01")
    }

    @Test
    fun feilIPeriode_erGyldigPeriode_false() {
        PeriodeRegler.feilIPeriode(DATO.plusMonths(6), DATO.plusYears(2)) shouldBe false
    }

    @Test
    fun feilIPeriode_tomFørFom_true() {
        PeriodeRegler.feilIPeriode(DATO.plusYears(1), DATO) shouldBe true
    }

    @Test
    fun periodeErÅpen_ingenTilDato_true() {
        PeriodeRegler.periodeErÅpen(DATO.plusMonths(6), null) shouldBe true
    }

    @Test
    fun periodeErÅpen_ikkeÅpen_false() {
        PeriodeRegler.periodeErÅpen(DATO.plusMonths(6), DATO.plusYears(1)) shouldBe false
    }

    @Test
    fun periodeOver24Mnd_periodeOver24Mnd_true() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusYears(3)) shouldBe true
    }

    @Test
    fun periodeOver24Mnd_periode24Mnd_true() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(24)) shouldBe true
    }

    @Test
    fun periodeOver24Mnd_periode23Mnd_false() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(23)) shouldBe false
    }

    @Test
    fun periodeOver24Mnd_periode14Mnd_false() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(14)) shouldBe false
    }

    @Test
    fun periodeOver3År_periode2År_false() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(2).plusMonths(11)) shouldBe false
    }

    @Test
    fun periodeOver3År_periode3År_true() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(3)) shouldBe true
    }

    @Test
    fun periodeOver3År_periode4År_true() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(4)) shouldBe true
    }

    @Test
    fun periodeOver5År_periode5År_true() {
        PeriodeRegler.periodeOver5År(DATO, DATO.plusYears(5)) shouldBe true
    }

    @Test
    fun periodeOver5År_periode4År_false() {
        PeriodeRegler.periodeOver5År(DATO, DATO.plusYears(4).plusMonths(11)) shouldBe false
    }

    @Test
    fun datoEldreEnn3År_dato6årSiden_true() {
        PeriodeRegler.datoEldreEnn3År(LocalDate.now().minusYears(6)) shouldBe true
    }

    @Test
    fun datoEldreEnn3År_datoFraNå_false() {
        PeriodeRegler.datoEldreEnn3År(LocalDate.now()) shouldBe false
    }

    @Test
    fun datoEldreEnn2Mnd_datoFraNå_false() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe false
    }

    @Test
    fun datoEldreEnn2Mnd_datoToMndSiden_false() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe false
    }

    @Test
    fun datoEldreEnn2Mnd_datoToMndSidenOgEnDag_true() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe true
    }

    @Test
    fun datoEldreEnn2Mnd_datoTreMndSiden_true() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(3).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe true
    }

    @Test
    fun periodeOver1ÅrFremITid_periodeOm2År_true() {
        PeriodeRegler.datoOver1ÅrFremITid(LocalDate.now().plusYears(2)) shouldBe true
    }

    @Test
    fun periodeOver1ÅrFremITid_periodeNå_false() {
        PeriodeRegler.datoOver1ÅrFremITid(DATO) shouldBe false
    }

    @Test
    fun periodeErLik_periodeLik_true() {
        PeriodeRegler.periodeErLik(DATO, DATO, DATO, DATO) shouldBe true
    }

    @Test
    fun periodeErLik_periodeIkkeLik_false() {
        PeriodeRegler.periodeErLik(DATO, DATO.plusYears(1), DATO, DATO) shouldBe false
    }

    @Test
    fun periodeErLik_tomErNullPeriodeLik_true() {
        PeriodeRegler.periodeErLik(DATO, null, DATO, null) shouldBe true
    }

    @Test
    fun periodeErLik_tom2ErNullPeriodeIkkeLik_false() {
        PeriodeRegler.periodeErLik(DATO, DATO, DATO, null) shouldBe false
    }

    @Test
    fun datoErFørFørsteJuni2012_datoI2020_false() {
        PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2020, 1, 1)) shouldBe false
    }

    @Test
    fun datoErFørFørsteJuni2012_dato01012012_true() {
        PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2012, 1, 1)) shouldBe true
    }

    @Test
    fun periodeOver12Måneder_periode12Måneder_true() {
        PeriodeRegler.periodeOver12Måneder(DATO, DATO.plusMonths(12)) shouldBe true
    }
} 